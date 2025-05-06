package org.apiwiz.scriptingengine.executor;

import org.apiwiz.scriptingengine.dto.ScriptResponse;
import org.apiwiz.scriptingengine.exception.ScriptExecutionException;
import org.apiwiz.scriptingengine.models.ScriptLanguage;
import org.apiwiz.scriptingengine.utils.NpmDependencyDownloader;
import org.apiwiz.scriptingengine.utils.NpmDependencyExtractor;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.apiwiz.scriptingengine.utils.NpmDependencyDownloader.DOWNLOAD_DIR;


@Component
public class JsScriptExecutor implements ScriptExecutor {
    public static final String NODE_MODULES = "deps/js/node_modules";

    @Override
    public ScriptResponse execute(ScriptLanguage language, String script) {
        try {
            // Ensure the directory exists before executing the script
            File downloadDir = new File(DOWNLOAD_DIR);
            if (!downloadDir.exists())
                downloadDir.mkdirs();

            // Step 1: Extract required NPM modules from script
            Set<String> requiredModules = NpmDependencyExtractor.extractRequiredModules(script);

            // Step 2: Download and install the required NPM modules
            if (!requiredModules.isEmpty())
                NpmDependencyDownloader.installDependencies(requiredModules);

            // Step 3: Create GraalVM JS context with require enabled
            try (Context context = createGraalVMContext()) {
                var result = context.eval(Source.newBuilder("js", script.strip(), "script.js").build());
                return new ScriptResponse(result.toString(), true);
            }

        } catch (Exception e) {
            throw new ScriptExecutionException("Error executing JavaScript script: " + e.getMessage(), e);
        }
    }

    @Override
    public ScriptResponse executeFromMultipartFile(ScriptLanguage language, MultipartFile file) {
        try {
            String scriptContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            return execute(language, scriptContent);
        } catch (IOException e) {
            throw new ScriptExecutionException("Failed to read uploaded JS file: " + e.getMessage(), e);
        }
    }

    @Override
    public ScriptLanguage getSupportedLanguage() {
        return ScriptLanguage.JAVASCRIPT;
    }

    private Context createGraalVMContext() {
        return Context.newBuilder("js")
                .option("js.commonjs-require", "true")
                .option("js.commonjs-require-cwd", NODE_MODULES)
                .allowIO(IOAccess.ALL)
                .allowAllAccess(true)
                .build();
    }
}
