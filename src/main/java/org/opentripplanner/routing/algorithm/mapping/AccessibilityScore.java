package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.model.plan.ScheduledTransitLeg;

public class AccessibilityScore {

    public static float compute(ScheduledTransitLeg leg) {
        var from = leg.getFrom().stop.getWheelchairBoarding();
        var to = leg.getFrom().stop.getWheelchairBoarding();
        var trip = leg.getTrip().getWheelchairBoarding();

        var sum = accessibilityScore(trip) + accessibilityScore(from) + accessibilityScore(to);
        return sum / 3;
    }

    public static float accessibilityScore(WheelChairBoarding wheelchair) {
        return switch (wheelchair) {
            case NO_INFORMATION -> 0.5f;
            case POSSIBLE -> 1f;
            case NOT_POSSIBLE -> 0f;
        };
    }
}
