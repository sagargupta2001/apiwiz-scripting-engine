package org.apiwiz.scriptingengine.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NpmDependencyExtractor {

    // Regular expression for matching `require('module')` statements
    private static final Pattern REQUIRE_PATTERN = Pattern.compile("require\\(['\"]([^'\"]+)['\"]\\)");

    public static Set<String> extractRequiredModules(String script) {
        Set<String> modules = new HashSet<>();
        Matcher matcher = REQUIRE_PATTERN.matcher(script);

        while (matcher.find()) {
            modules.add(matcher.group(1)); // Add the module name
        }

        return modules;
    }
}
