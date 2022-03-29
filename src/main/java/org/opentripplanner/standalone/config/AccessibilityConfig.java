package org.opentripplanner.standalone.config;

import static org.opentripplanner.model.AccessibilityRequirements.DEFAULT_INACCESSIBLE_STOP_COST;
import static org.opentripplanner.model.AccessibilityRequirements.DEFAULT_INACCESSIBLE_TRIP_COST;
import static org.opentripplanner.model.AccessibilityRequirements.DEFAULT_UNKNOWN_ACCESSIBILITY_TRIP_COST;
import static org.opentripplanner.model.AccessibilityRequirements.DEFAULT_UNKNOWN_STOP_ACCESSIBILITY_COST;

import org.opentripplanner.model.AccessibilityRequirements;
import org.opentripplanner.model.AccessibilityRequirements.EvaluationType;

public record AccessibilityConfig(Evaluation evaluation,
                                  int unknownTripAccessibilityCost,
                                  int inaccessibleTripCost,
                                  int unknownStopAccessibilityCost,
                                  int inaccessibleStopCost) {

    public AccessibilityConfig(NodeAdapter adapter) {
        this(
                adapter.asEnum("evaluation", Evaluation.KNOWN_INFORMATION_ONLY),
                adapter.asInt("unknownTripAccessibilityCost", DEFAULT_UNKNOWN_ACCESSIBILITY_TRIP_COST),
                adapter.asInt("inaccessibleTripCost", DEFAULT_INACCESSIBLE_TRIP_COST),
                adapter.asInt("unknownStopInaccessibilityCost", DEFAULT_UNKNOWN_STOP_ACCESSIBILITY_COST),
                adapter.asInt("inaccessibleStopCost", DEFAULT_INACCESSIBLE_STOP_COST)

        );
    }

    public AccessibilityRequirements toRequirements(AccessibilityRequirements fromRequest) {
        EvaluationType evaluation;

        if (fromRequest.requestsWheelchair()
                && this.evaluation == Evaluation.KNOWN_INFORMATION_ONLY) {
            evaluation = EvaluationType.KNOWN_INFORMATION_ONLY;
        }
        else if (fromRequest.requestsWheelchair()
                && this.evaluation == Evaluation.ALLOW_UNKNOWN_INFORMATION) {
            evaluation = EvaluationType.ALLOW_UNKNOWN_INFORMATION;
        }
        else {
            evaluation = EvaluationType.NOT_REQUIRED;
        }

        return new AccessibilityRequirements(
                evaluation,
                unknownTripAccessibilityCost,
                inaccessibleTripCost,
                unknownStopAccessibilityCost,
                inaccessibleStopCost
        );
    }

    public enum Evaluation {
        KNOWN_INFORMATION_ONLY,
        ALLOW_UNKNOWN_INFORMATION
    }
}
