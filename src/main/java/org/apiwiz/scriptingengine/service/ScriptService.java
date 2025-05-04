package org.apiwiz.scriptingengine.service;

import org.springframework.stereotype.Service;
import org.apiwiz.scriptingengine.dto.ScriptResponse;
import org.apiwiz.scriptingengine.executor.ScriptExecutor;
import org.apiwiz.scriptingengine.models.ScriptLanguage;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ScriptService {
    private final ScriptExecutor executor;

    public ScriptService(ScriptExecutor executor) {
        this.executor = executor;
    }

    public ScriptResponse executeScript(String language, String script) {
        return executor.execute(ScriptLanguage.fromString(language), script);
    }

    public ScriptResponse executeUploadedScriptFile(String language, MultipartFile file) {
        return executor.executeFromMultipartFile(ScriptLanguage.fromString(language), file);
    }
}
