package org.apiwiz.scriptingengine.service;

import org.springframework.stereotype.Service;
import org.apiwiz.scriptingengine.dto.ScriptRequest;
import org.apiwiz.scriptingengine.dto.ScriptResponse;
import org.apiwiz.scriptingengine.executor.ScriptExecutor;
import org.apiwiz.scriptingengine.models.ScriptLanguage;

@Service
public class ScriptService {
    private final ScriptExecutor executor;

    public ScriptService(ScriptExecutor executor) {
        this.executor = executor;
    }

    public ScriptResponse executeScript(String language, String script) {
        return executor.execute(ScriptLanguage.fromString(language), script);
    }
}
