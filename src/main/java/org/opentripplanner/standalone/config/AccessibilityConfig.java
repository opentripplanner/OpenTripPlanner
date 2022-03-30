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
                adapter.asInt("unknownStopAccessibilityCost", DEFAULT_UNKNOWN_STOP_ACCESSIBILITY_COST),
                adapter.asInt("inaccessibleStopCost", DEFAULT_INACCESSIBLE_STOP_COST)

        );
    }

    /**
     * What wheelchair=true means is configurable.
     * <p>
     * I can mean that you only want to use stops and trips that are known to be accessible.
     * <p>
     * I can also be configured to allow unknown information or even stops/trips that are known to
     * be inaccessible as a last resort.
     * <p>
     * Here we take the level of wheelchair accessibility that the user has selected (on/off) and
     * convert it to the accessibility requirements that are configured in router-config.json. These
     * requirements also include the configured costs for the various items of inaccessibility.
     * <p>
     * More documentation is at docs/Accessibility.md
     */
    public AccessibilityRequirements toRequirements(boolean wheelchair) {
        EvaluationType evaluation;

        if (wheelchair && this.evaluation == Evaluation.KNOWN_INFORMATION_ONLY) {
            evaluation = EvaluationType.KNOWN_INFORMATION_ONLY;
        }
        else if (wheelchair && this.evaluation == Evaluation.ALLOW_UNKNOWN_INFORMATION) {
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
