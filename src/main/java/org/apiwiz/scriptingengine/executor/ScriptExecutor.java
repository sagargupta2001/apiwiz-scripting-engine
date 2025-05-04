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

import java.util.List;
import java.util.Set;

@Component
public class ScriptExecutor {

    public ScriptResponse execute(ScriptLanguage language, String script) {
        try {
            // 1. Extract import statements
            Set<String> imports = PythonImportExtractor.extractImports(script);

            // 2. Download missing packages from PyPI
            PyPIDependencyDownloader.installDependencies(imports);

            // 3. Resolve all importable paths
            List<String> modulePaths = PyPIDependencyDownloader.getImportableModulePaths();

            // 4. Build sys.path bootstrap script
            StringBuilder sysPathBootstrap = new StringBuilder("import sys\nprint('Before sys.path:', sys.path)\n");
            for (String path : modulePaths) {
                sysPathBootstrap.append("sys.path.insert(0, '").append(path).append("')\n");
            }
            sysPathBootstrap.append("print('After sys.path:', sys.path)");

            try (var context = Context.newBuilder(language.getEngineName())
                    .allowIO(IOAccess.ALL)
                    .build()) {

                // 5. Inject sys.path and execute user script
                context.eval(Source.newBuilder("python", sysPathBootstrap.toString(), "bootstrap.py").build());
                var result = context.eval(Source.newBuilder("python", script.strip(), "script.py").build());

                return new ScriptResponse(result.toString(), true);
            }

        } catch (Exception e) {
            throw new ScriptExecutionException("Error executing script: " + e.getMessage(), e);
        }
    }
}
