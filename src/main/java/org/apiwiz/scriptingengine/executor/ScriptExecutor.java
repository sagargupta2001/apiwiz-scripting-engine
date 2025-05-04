package org.apiwiz.scriptingengine.executor;

import org.apiwiz.scriptingengine.dto.ScriptResponse;
import org.apiwiz.scriptingengine.models.ScriptLanguage;
import org.springframework.web.multipart.MultipartFile;

public interface ScriptExecutor {
    ScriptResponse execute(ScriptLanguage script, String language);
    ScriptResponse executeFromMultipartFile(ScriptLanguage language, MultipartFile file);
    ScriptLanguage getSupportedLanguage();
}
