package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.opentripplanner.standalone.config.JsonSupport.jsonNodeForTest;


public class BuildConfigTest {
    @Test
    public void testParsePeriodDate() {
        // Given
        JsonNode node  = jsonNodeForTest("{ 'parentStopLinking' : true }");

        BuildConfig subject = new BuildConfig(node, "Test", false);

        // Then
        assertTrue(subject.parentStopLinking);
    }
}