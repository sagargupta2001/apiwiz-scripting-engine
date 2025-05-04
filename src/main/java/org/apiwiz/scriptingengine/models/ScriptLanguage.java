package org.apiwiz.scriptingengine.models;

import org.apiwiz.scriptingengine.exception.UnsupportedLanguageException;

public enum ScriptLanguage {
    PYTHON("python"),
    JAVASCRIPT("js");

    private final String engineName;

    ScriptLanguage(String engineName) {
        this.engineName = engineName;
    }

    public String getEngineName() {
        return engineName;
    }

    public static ScriptLanguage fromString(String value) {
        var language = value.toLowerCase();
        return switch (language) {
            case "python" -> PYTHON;
            case "js", "javascript" -> JAVASCRIPT;
            default ->  throw new UnsupportedLanguageException(language);
        };
    }
}
