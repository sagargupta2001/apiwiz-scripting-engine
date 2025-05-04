package org.apiwiz.scriptingengine.exception;

public class UnsupportedLanguageException extends RuntimeException {
    public UnsupportedLanguageException(String language) {
        super("Unsupported scripting language: " + language);
    }
}
