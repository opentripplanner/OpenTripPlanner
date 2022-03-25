package org.opentripplanner.standalone.config;

import org.opentripplanner.model.AccessibilityRequirements;
import org.opentripplanner.model.AccessibilityRequirements.EvaluationType;

public record AccessibilityConfig(Evaluation evaluation) {

    public AccessibilityConfig(NodeAdapter adapter) {
        this(adapter.asEnum("evaluation", Evaluation.KNOWN_INFORMATION_ONLY));
    }

    public AccessibilityRequirements toRequirements(AccessibilityRequirements fromRequest) {
        EvaluationType evaluation;

        if (fromRequest.requestsWheelchair() && this.evaluation == Evaluation.KNOWN_INFORMATION_ONLY) {
            evaluation = EvaluationType.KNOWN_INFORMATION_ONLY;
        }
        else if (fromRequest.requestsWheelchair() && this.evaluation == Evaluation.ALLOW_UNKNOWN_INFORMATION) {
            evaluation = EvaluationType.ALLOW_UNKNOWN_INFORMATION;
        }
        else {
            evaluation = EvaluationType.NOT_REQUIRED;
        }

        return new AccessibilityRequirements(
                evaluation,
                fromRequest.unknownAccessibilityTripCost(),
                fromRequest.inaccessibleTripCost(),
                fromRequest.unknownStopAccessibilityCost(),
                fromRequest.inaccessibleStopCost()
        );
    }

    public enum Evaluation {
        KNOWN_INFORMATION_ONLY,
        ALLOW_UNKNOWN_INFORMATION
    }
}
