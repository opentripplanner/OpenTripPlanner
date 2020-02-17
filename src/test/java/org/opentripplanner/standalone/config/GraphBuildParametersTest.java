package org.opentripplanner.standalone.config;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.opentripplanner.standalone.config.NodeAdapterTest.newNodeAdapterForTest;


public class GraphBuildParametersTest {
    @Test
    public void testParsePeriodDate() {
        // Given
        NodeAdapter nodeAdapter  = newNodeAdapterForTest("{ 'parentStopLinking' : true }");

        GraphBuildParameters subject = new GraphBuildParameters(nodeAdapter);

        // Then
        assertTrue(subject.parentStopLinking);
    }
}