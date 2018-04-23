package org.opentripplanner.standalone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;


import java.util.List;

import static java.util.Arrays.asList;
import static org.opentripplanner.standalone.GraphBuilderParameters.valueOf;
import static org.junit.Assert.*;

public class GraphBuilderParametersTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String KEY = "key";
    private static final String A = "A";
    private static final String B = "B";
    private static final String DEFAULT = B;
    private static final List<String> A_B = asList(A, B);
    private static final List<String> B_C = asList(B, "C");


    @Test
    public void testValueOf() throws Exception {
        // Given
        JsonNode config = readConfig("{ 'key' : 'A' }");

        // Then
        assertEquals("Get existing property", A, valueOf(config, KEY, DEFAULT, A_B));
        assertEquals("Get default value", DEFAULT, valueOf(config, "missing-key", DEFAULT, B_C));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfWithIllegalPropertySet() throws Exception {
        // Given
        JsonNode config = readConfig("{ 'key' : 'X' }");

        // Then expect an error when value 'X' is not in the set of legal values: ['A', 'B']
        valueOf(config, "key", DEFAULT, A_B);
    }

    private static JsonNode readConfig(String text) throws Exception {
        return OBJECT_MAPPER.readTree(text.replace('\'', '"'));
    }
}