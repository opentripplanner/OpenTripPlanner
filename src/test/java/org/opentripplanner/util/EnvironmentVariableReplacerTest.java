package org.opentripplanner.util;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.standalone.config.ConfigLoader;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EnvironmentVariableReplacerTest {


    private String envName = "<not set>";
    private String envValue = "<not set>";


    /**
     * To test the system environment variable substitution we must have a name/value pair that
     * exist in the system environment variables. So to prepare for the test, we look up a random
     * environment variable and use it later to construct the test samples, and the expected
     * results.
     *
     * We search for a environment variable name containing only alphanumeric characters and a
     * value with less than 30 characters. We do this to make it easier for humans to see what is
     * going on, if a test fails. This constraint is just to make the text involved more
     * readable.
     */
    @Before
    public void setup() {
        Map.Entry<String, String> envVar = System.getenv().entrySet()
                .stream()
                .filter(e ->
                        e.getKey().matches("\\w+") && e.getValue().length() < 30
                )
                .findFirst()
                .orElse(null);

        if(envVar == null) {
            throw new IllegalStateException("No environment variables for testing found.");
        }
        envName = envVar.getKey();
        envValue = envVar.getValue();
    }


    /**
     * Test replacing environment variables in a text. The {@link EnvironmentVariableReplacer}
     * should replace placeholders like '${ENV_NAME}' with the value of the system environment
     * variable.
     */
    @Test
    public void insertEnvironmentVariables() {
        // Given: a text and a expected result
        String text = "Env.var: ${" + envName + "}.";
        String expectedResult = "Env.var: " + envValue + ".";

        // When:
        String result = EnvironmentVariableReplacer.insertEnvironmentVariables(text, "test");

        // Then:
        assertEquals(expectedResult, result);
    }

    @Test
    public void verifyThatAEnvVariableMayExistMoreThanOnce() {
        // Given: a text and a expected result
        String text = "Env. var1: ${" + envName + "} and var2: ${" + envName + "}.";
        String expectedResult = "Env. var1: " + envValue + " and var2: " + envValue + ".";

        // When:
        String result = EnvironmentVariableReplacer.insertEnvironmentVariables(text, "test");

        // Then:
        assertEquals(expectedResult, result);
    }


    /**
     * Test replacing environment variable fails for unknown environment variable.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMissingEnvironmentVariable() {
        ConfigLoader.fromString(
                "None existing env.var: '${none_existing_env_variable}'.", "test"
        );
    }
}