package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opentripplanner.util.OtpAppException;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.standalone.config.GraphBuildParameters.enumValueOf;
import static org.opentripplanner.standalone.config.GraphBuildParameters.parseDateOrRelativePeriod;
import static org.opentripplanner.standalone.config.GraphBuildParametersTest.AnEnum.A;
import static org.opentripplanner.standalone.config.GraphBuildParametersTest.AnEnum.B;


public class GraphBuildParametersTest {
    enum AnEnum { A, B }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String KEY = "key";
    private static final AnEnum DEFAULT = B;


    @Test
    public void testValueOf() throws Exception {
        // Given
        JsonNode config = readConfig("{ 'key' : 'A' }");

        // Then
        assertEquals("Get existing property", A, enumValueOf(config, KEY, DEFAULT));
        assertEquals("Get default value", DEFAULT, enumValueOf(config, "missing-key", DEFAULT));
    }

    @Test(expected = OtpAppException.class)
    public void testValueOfWithIllegalPropertySet() throws Exception {
        // Given
        JsonNode config = readConfig("{ 'key' : 'X' }");

        // Then expect an error when value 'X' is not in the set of legal values: ['A', 'B']
        enumValueOf(config, "key", DEFAULT);
    }


    @Test
    public void testParsePeriodDate() throws Exception {
        // Given
        JsonNode config = readConfig("{ 'a' : '2020-02-28', 'b' : '-P3Y' }");

        // Then
        assertEquals(LocalDate.of(2020, 2, 28), parseDateOrRelativePeriod(config, "a", null));
        assertEquals(LocalDate.now().minusYears(3), parseDateOrRelativePeriod(config, "b", null));
        assertEquals(LocalDate.of(2020, 3, 1), parseDateOrRelativePeriod(config, "do-no-exist", "2020-03-01"));
        assertNull(parseDateOrRelativePeriod(config, "do-no-exist", null));
    }

    @Test(expected = OtpAppException.class)
    public void testParsePeriodDateThrowsException() throws Exception {
        // Given
        JsonNode config = readConfig("{ 'foo' : 'bar' }");

        // Then
        parseDateOrRelativePeriod(config, "foo", null);
    }

    private static JsonNode readConfig(String text) throws Exception {
        return OBJECT_MAPPER.readTree(text.replace('\'', '"'));
    }
}