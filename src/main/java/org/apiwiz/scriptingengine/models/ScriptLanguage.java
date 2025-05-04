package org.apiwiz.scriptingengine.models;

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
        return switch (value.toLowerCase()) {
            case "python" -> PYTHON;
            case "js", "javascript" -> JAVASCRIPT;
            default -> throw new IllegalArgumentException("Unsupported language: " + value);
        };
    }
}
