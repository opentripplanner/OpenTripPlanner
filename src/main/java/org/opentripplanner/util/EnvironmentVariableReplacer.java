package org.opentripplanner.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces variables specified on the format ${variable} in a string with current system
 * environment variables
 */
public class EnvironmentVariableReplacer {

    private static Pattern PATTERN = Pattern.compile("\\$\\{(\\w+)\\}|\\$(\\w+)");


    public String replace(String s) {
        Map<String, String> environmentVariables = new HashMap<>();
        Matcher matcher = PATTERN.matcher(s);

        while (matcher.find()) {
            String envVar = matcher.group(0);
            String nameOnly = matcher.group(1);
            if (!environmentVariables.containsKey(nameOnly)) {
                String value = System.getenv(nameOnly);
                if (value != null) {
                    environmentVariables.put(envVar, value);
                }
                else {
                    throw new IllegalArgumentException(
                            "Environment variable " + nameOnly + " not specified"
                    );
                }
            }
        }
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            s = s.replace(entry.getKey(), entry.getValue());
        }
        return s;
    }
}
