package org.opentripplanner.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces environment variable placeholders specified on the format ${variable} in a text
 * with the current system environment variable values.
 */
public class EnvironmentVariableReplacer {
    /**
     * A pattern matching a placeholder like '${VAR_NAME}'. The placeholder must start with
     * '${' and end with '}'. The environment variable name must consist of only alphanumerical
     * characters(a-z, A-Z, 0-9) and underscore '_'.
     */
    private static Pattern PATTERN = Pattern.compile("\\$\\{(\\w+)}");


    /**
     * Search for {@link #PATTERN}s and replace each placeholder with the value of the
     * corresponding environment variable.
     *
     * @param source is used only to generate human friendly error message in case the text
     *               contain a placeholder whitch can not be found.
     * @throws IllegalArgumentException if a placeholder exist in the {@code text}, but the
     *                                  environment variable do not exist.
     */
    public static String insertEnvironmentVariables(String text, String source) {
        Map<String, String> environmentVariables = new HashMap<>();
        Matcher matcher = PATTERN.matcher(text);

        while (matcher.find()) {
            String envVar = matcher.group(0);
            String nameOnly = matcher.group(1);
            if (!environmentVariables.containsKey(nameOnly)) {
                String value = System.getenv(nameOnly);
                if (value != null) {
                    environmentVariables.put(envVar, value);
                }
                else {
                    throw new OtpAppException(
                            "Environment variable name '" + nameOnly + "' in config '"
                            + source + "' not found in the system environment variables."
                    );
                }
            }
        }
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }
}
