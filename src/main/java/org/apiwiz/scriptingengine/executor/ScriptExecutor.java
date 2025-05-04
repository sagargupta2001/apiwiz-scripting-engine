package org.apiwiz.scriptingengine.executor;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.stereotype.Component;
import org.apiwiz.scriptingengine.dto.ScriptResponse;
import org.apiwiz.scriptingengine.exception.ScriptExecutionException;
import org.apiwiz.scriptingengine.models.ScriptLanguage;

@Component
public class ScriptExecutor {
    public ScriptResponse execute(ScriptLanguage language, String script) {
        try (var context = Context.newBuilder(language.getEngineName())
                .allowAllAccess(false)
                .allowIO(IOAccess.NONE)
                .build()) {
            var result = context.eval(language.getEngineName(), script);
            return new ScriptResponse(result.toString(), true);
        } catch (Exception e) {
            throw new ScriptExecutionException("Error executing script: " + e.getMessage(), e);
        }
    }
}
