package org.opentripplanner.model;

import static org.opentripplanner.model.AccessibilityRequirements.Strictness.ALLOW_UNKNOWN_INFORMATION;
import static org.opentripplanner.model.AccessibilityRequirements.Strictness.KNOWN_INFORMATION_ONLY;

public record AccessibilityRequirements(Strictness strictness,
                                        int unknownAccessibilityTripCost,
                                        int inaccessibleTripCost,
                                        int unknownStopCost) {

    public static final int UNKNOWN_ACCESSIBILITY_TRIP_COST = 5 * 60;
    public static final int INACCESSIBLE_TRIP_COST = 60 * 60;
    public static final int UNKNOWN_STOP_COST = 60 * 60;

    public enum Strictness {
        // accessibility information doesn't play a role in routing
        NOT_REQUIRED,
        // only routes and places, which are known to be wheelchair-accessible, should be used
        KNOWN_INFORMATION_ONLY,
        // trips/stops that are known to be wheelchair-accessible are preferred but those with unknown
        // information are also allowed (but receive extra cost)
        // it even allows you to use stops/trips which are known to be inaccessible (!) but at
        // a very severe cost
        ALLOW_UNKNOWN_INFORMATION;
    }

    public boolean requestsWheelchair() {
        return strictness == KNOWN_INFORMATION_ONLY || strictness == ALLOW_UNKNOWN_INFORMATION;
    }

    public static AccessibilityRequirements makeDefault(Strictness strictness) {
        return new AccessibilityRequirements(
                strictness,
                UNKNOWN_ACCESSIBILITY_TRIP_COST,
                INACCESSIBLE_TRIP_COST,
                UNKNOWN_STOP_COST
        );
    }
}
