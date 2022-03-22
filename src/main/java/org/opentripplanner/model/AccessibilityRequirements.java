package org.opentripplanner.model;

public enum AccessibilityRequirements {
    // accessibility information doesn't play a role in routing
    NOT_REQUIRED,
    // only routes and places, which are known to be wheelchair-accessible, should be used
    KNOWN_INFORMATION_ONLY,
    // trips/stops that are known to be wheelchair-accessible are preferred but those with unknown
    // information are also allowed (but receive extra cost)
    ALLOW_UNKNOWN_INFORMATION;

    public boolean requestsWheelchair() {
        return this == KNOWN_INFORMATION_ONLY || this == ALLOW_UNKNOWN_INFORMATION;
    }
}
