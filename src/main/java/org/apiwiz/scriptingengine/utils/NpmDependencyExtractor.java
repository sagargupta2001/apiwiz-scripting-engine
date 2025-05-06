package org.apiwiz.scriptingengine.utils;

import java.util.*;
import java.util.regex.*;

public class NpmDependencyExtractor {
    private static final Pattern REQUIRE  = Pattern.compile("require\\(['\"]([^'\"]+)['\"]\\)");
    private static final Pattern IMPORT   = Pattern.compile(
            "import\\s+(?:[^;]+?)\\s+from\\s+['\"]([^'\"]+)['\"]"
    );

    public static Set<String> extractRequiredModules(String script) {
        Set<String> pkgs = new HashSet<>();

        Matcher m1 = REQUIRE.matcher(script);
        while (m1.find()) {
            pkgs.add(m1.group(1));
        }

        Matcher m2 = IMPORT.matcher(script);
        while (m2.find()) {
            pkgs.add(m2.group(1));
        }

        return pkgs;
    }
}
