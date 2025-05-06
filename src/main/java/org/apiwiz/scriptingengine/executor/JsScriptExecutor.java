package org.apiwiz.scriptingengine.executor;

import org.apiwiz.scriptingengine.dto.ScriptResponse;
import org.apiwiz.scriptingengine.exception.ScriptExecutionException;
import org.apiwiz.scriptingengine.models.ScriptLanguage;
import org.apiwiz.scriptingengine.utils.NpmDependencyDownloader;
import org.apiwiz.scriptingengine.utils.NpmDependencyExtractor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Component
public class JsScriptExecutor implements ScriptExecutor {
    /** Parent directory under which node_modules will be created */
    public static final String MODULE_ROOT = "deps/js";

    @Override
    public ScriptResponse execute(ScriptLanguage language, String script) {
        try {
            // 0) Ensure module root exists
            File root = new File(MODULE_ROOT);
            if (!root.exists() && !root.mkdirs()) {
                throw new IOException("Could not create module root: " + MODULE_ROOT);
            }

            // 1) Extract bare specifier dependencies (require/import)
            Set<String> required = NpmDependencyExtractor.extractRequiredModules(script);
            System.out.println("Dependencies to install: " + required);

            // 2) Block until download + extraction + patching completes
            if (!required.isEmpty())
                NpmDependencyDownloader.installDependencies(required);


            // 3) Create GraalVM JS context pointed at MODULE_ROOT
            try (Context context = createGraalVMContext()) {
                // 4) Polyfill fetch() so HTTP‑based libs work inside JVM
                context.eval("js",
                        "globalThis.fetch = (url, opts) => {" +
                                " const HttpClient = Java.type('java.net.http.HttpClient');" +
                                " const HttpRequest = Java.type('java.net.http.HttpRequest');" +
                                " const BodyHandlers = Java.type('java.net.http.HttpResponse$BodyHandlers');" +
                                " let builder = HttpRequest.newBuilder().uri(java.net.URI.create(url));" +
                                " let response = HttpClient.newBuilder().build()" +
                                "                   .send(builder.build(), BodyHandlers.ofString());" +
                                " return Promise.resolve({ " +
                                "   text: () => response.body()," +
                                "   json: () => JSON.parse(response.body())" +
                                " });" +
                                "};"
                );

                // 5) Evaluate user script as ES‑module so `import … from 'x'` works
                Source src = Source.newBuilder("js", script.strip(), "user-script.mjs")
                        .mimeType("application/javascript+module")
                        .build();
                Value result = context.eval(src);

                return new ScriptResponse(result.toString(), true);
            }

        } catch (Exception e) {
            throw new ScriptExecutionException("Error executing JavaScript script: " + e.getMessage(), e);
        }
    }

    @Override
    public ScriptResponse executeFromMultipartFile(ScriptLanguage language, MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return execute(language, content);
        } catch (IOException e) {
            throw new ScriptExecutionException("Failed to read uploaded JS file: " + e.getMessage(), e);
        }
    }

    @Override
    public ScriptLanguage getSupportedLanguage() {
        return ScriptLanguage.JAVASCRIPT;
    }

    /**
     * Build a GraalVM Context that:
     *  - Allows filesystem require()/import from MODULE_ROOT
     *  - Supports CommonJS require
     *  - Resolves bare specifiers like Node.js
     */
    private Context createGraalVMContext() {
        String absRoot = new File(MODULE_ROOT).getAbsolutePath();
        String nodeModulesPath = new File(absRoot, "node_modules").getAbsolutePath();
        Path cwd = Paths.get(absRoot);

        return Context.newBuilder("js")
                .allowIO(IOAccess.ALL)
                .allowAllAccess(true)
                .allowExperimentalOptions(true)
                .option("js.commonjs-require", "true")
                .option("js.commonjs-require-cwd", absRoot)
                .option("js.esm-bare-specifier-relative-lookup", "true")
                .option("js.esm-eval-returns-exports", "true")
                .environment("NODE_PATH", nodeModulesPath)
                .currentWorkingDirectory(cwd)
                .build();
    }
}
