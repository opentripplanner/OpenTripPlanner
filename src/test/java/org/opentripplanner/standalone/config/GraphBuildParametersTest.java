package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;


import static org.opentripplanner.standalone.config.GraphBuildParameters.enumValueOf;
import static org.junit.Assert.*;
import static org.opentripplanner.standalone.config.GraphBuildParametersTest.AnEnum.A;


public class GraphBuildParametersTest {
    enum AnEnum { A, B }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String KEY = "key";
    private static final AnEnum DEFAULT = AnEnum.B;


    @Test
    public void testValueOf() throws Exception {
        // Given
        JsonNode config = readConfig("{ 'key' : 'A' }");

        // Then
        assertEquals("Get existing property", A, enumValueOf(config, KEY, DEFAULT));
        assertEquals("Get default value", DEFAULT, enumValueOf(config, "missing-key", DEFAULT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfWithIllegalPropertySet() throws Exception {
        // Given
        JsonNode config = readConfig("{ 'key' : 'X' }");

        // Then expect an error when value 'X' is not in the set of legal values: ['A', 'B']
        enumValueOf(config, "key", DEFAULT);
    }

    private static JsonNode readConfig(String text) throws Exception {
        return OBJECT_MAPPER.readTree(text.replace('\'', '"'));
    }
}