package org.apiwiz.scriptingengine.service;

import org.apiwiz.scriptingengine.dto.ScriptResponse;
import org.apiwiz.scriptingengine.executor.ScriptExecutor;
import org.apiwiz.scriptingengine.exception.ScriptExecutionException;
import org.apiwiz.scriptingengine.models.ScriptLanguage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class ScriptService {

    private final Map<ScriptLanguage, ScriptExecutor> executors;

    public ScriptService(Map<ScriptLanguage, ScriptExecutor> executors) {
        this.executors = executors;
    }

    public ScriptResponse executeScript(String language, String script) {
        ScriptLanguage lang = ScriptLanguage.fromString(language);
        ScriptExecutor executor = resolveExecutor(lang);
        return executor.execute(lang, script);
    }

    public ScriptResponse executeUploadedScriptFile(String language, MultipartFile file) {
        ScriptLanguage lang = ScriptLanguage.fromString(language);
        ScriptExecutor executor = resolveExecutor(lang);
        return executor.executeFromMultipartFile(lang, file);
    }

    private ScriptExecutor resolveExecutor(ScriptLanguage language) {
        ScriptExecutor executor = executors.get(language);
        if (executor == null) {
            throw new ScriptExecutionException("Unsupported language: " + language.name(), null);
        }
        return executor;
    }
}
