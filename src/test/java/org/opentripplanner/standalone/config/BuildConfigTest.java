package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.annotation.ComponentAnnotationConfigurator;

import static org.junit.Assert.assertTrue;
import static org.opentripplanner.standalone.config.JsonSupport.jsonNodeForTest;


public class BuildConfigTest {

    @Before
    public void setUp() throws IOException {
        ComponentAnnotationConfigurator.getInstance().fromConfig(null);
        // Create a base path for this test - correspond to OTP BASE_PATH
    }

    @Test
    public void testParsePeriodDate() {
        // Given
        JsonNode node  = jsonNodeForTest("{ 'parentStopLinking' : true }");

        BuildConfig subject = new BuildConfig(node, "Test");

        // Then
        assertTrue(subject.parentStopLinking);
    }
}
