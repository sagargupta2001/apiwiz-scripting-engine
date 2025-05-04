package org.apiwiz.scriptingengine.executor;

import org.apiwiz.scriptingengine.dto.ScriptResponse;
import org.apiwiz.scriptingengine.exception.ScriptExecutionException;
import org.apiwiz.scriptingengine.models.ScriptLanguage;
import org.apiwiz.scriptingengine.utils.PythonImportExtractor;
import org.apiwiz.scriptingengine.utils.PyPIDependencyDownloader;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class PythonScriptExecutor implements ScriptExecutor{

    @Override
    public ScriptResponse execute(ScriptLanguage language, String script) {
        return executeWithContent(language, script, "script.py");
    }

    public ScriptResponse executeFromMultipartFile(ScriptLanguage language, MultipartFile file) {
        try {
            String scriptContent = new String(file.getBytes());
            return executeWithContent(language, scriptContent, file.getOriginalFilename());
        } catch (IOException e) {
            throw new ScriptExecutionException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    @Override
    public ScriptLanguage getSupportedLanguage() {
        return ScriptLanguage.PYTHON;
    }

    private ScriptResponse executeWithContent(ScriptLanguage language, String script, String sourceName) {
        try {
            Set<String> imports = PythonImportExtractor.extractImports(script);
            PyPIDependencyDownloader.installDependencies(imports);
            List<String> modulePaths = PyPIDependencyDownloader.getImportableModulePaths();

            StringBuilder sysPathBootstrap = new StringBuilder("import sys\nprint('Before sys.path:', sys.path)\n");
            for (String path : modulePaths) {
                sysPathBootstrap.append("sys.path.insert(0, '").append(path).append("')\n");
            }
            sysPathBootstrap.append("print('After sys.path:', sys.path)");

            try (var context = Context.newBuilder(language.getEngineName())
                    .allowIO(IOAccess.ALL)
                    .build()) {

                context.eval(Source.newBuilder("python", sysPathBootstrap.toString(), "bootstrap.py").build());
                var result = context.eval(Source.newBuilder("python", script.strip(), sourceName).build());
                return new ScriptResponse(result.toString(), true);
            }
        } catch (Exception e) {
            throw new ScriptExecutionException("Error executing script: " + e.getMessage(), e);
        }
    }
}
