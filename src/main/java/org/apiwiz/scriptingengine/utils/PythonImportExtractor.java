package org.apiwiz.scriptingengine.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonImportExtractor {

    // Regex pattern to match both 'import module' and 'from module import ...' statements
    private static final Pattern IMPORT_PATTERN =
            Pattern.compile("(?m)^\\s*(?:import|from)\\s+([a-zA-Z_][a-zA-Z0-9_]*)(?:\\s+import)?");

    // List of standard built-in modules to exclude from the dependency download
    private static final Set<String> STANDARD_LIBRARIES = Set.of(
            "sys", "os", "math", "json", "re", "time", "itertools", "datetime", "subprocess", "builtins"
    );

    /**
     * Extracts external Python module imports from the given script.
     * Excludes built-in modules from the result.
     *
     * @param script the Python script to extract imports from
     * @return a set of external modules to be installed
     */
    public static Set<String> extractImports(String script) {
        Matcher matcher = IMPORT_PATTERN.matcher(script);
        Set<String> modules = new HashSet<>();

        while (matcher.find()) {
            String fullImport = matcher.group(1);
            String baseModule = fullImport.split("\\.")[0];  // Handle imports like 'from package.submodule import ...'

            // Add module only if it is not a standard library
            if (!STANDARD_LIBRARIES.contains(baseModule)) {
                modules.add(baseModule);
            }
        }

        return modules;
    }
}
