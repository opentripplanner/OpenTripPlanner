package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.JsonSupport.newNodeAdapterForTest;

import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.AccessibilityConfig.Evaluation;

class AccessibilityConfigTest {

    @Test
    public void parseDefault() {
        var subject = new AccessibilityConfig(
                new NodeAdapter(MissingNode.getInstance(), this.getClass().getSimpleName())
        );

        assertEquals(600, subject.unknownStopAccessibilityCost());
    }

    @Test
    public void loadFromJson() {
        var nodeAdapter = newNodeAdapterForTest(
                """
                        {
                            "evaluation": "ALLOW_UNKNOWN_INFORMATION",
                            "unknownStopAccessibilityCost": 10,
                            "inaccessibleStopCost" : 2000,
                            "unknownTripAccessibilityCost": 30,
                            "inaccessibleTripCost": 3000
                        }
                        """
        );

        var subject = new AccessibilityConfig(nodeAdapter);

        assertEquals(Evaluation.ALLOW_UNKNOWN_INFORMATION, subject.evaluation());
        assertEquals(10, subject.unknownStopAccessibilityCost());
        assertEquals(2000, subject.inaccessibleStopCost());
        assertEquals(30, subject.unknownTripAccessibilityCost());
        assertEquals(3000, subject.inaccessibleTripCost());
    }
}