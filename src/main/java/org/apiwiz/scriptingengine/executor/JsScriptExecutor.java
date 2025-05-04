package org.apiwiz.scriptingengine.executor;

import org.apiwiz.scriptingengine.dto.ScriptResponse;
import org.apiwiz.scriptingengine.exception.ScriptExecutionException;
import org.apiwiz.scriptingengine.models.ScriptLanguage;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class JsScriptExecutor implements ScriptExecutor{
    @Override
    public ScriptResponse execute(ScriptLanguage script, String language) {
        try (Context context = Context.newBuilder("js")
                .allowIO(IOAccess.ALL)
                .build()) {
            var result = context.eval(Source.newBuilder("js", script.getEngineName().strip(), language).build());
            return new ScriptResponse(result.toString(), true);
        } catch (Exception e) {
            throw new ScriptExecutionException("Error executing JavaScript script: " + e.getMessage(), e);
        }
    }

    @Override
    public ScriptResponse executeFromMultipartFile(ScriptLanguage language, MultipartFile file) {
        return null;
    }

    @Override
    public ScriptLanguage getSupportedLanguage() {
        return ScriptLanguage.JAVASCRIPT;
    }
}
