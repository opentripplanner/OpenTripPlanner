package org.opentripplanner.routing.algorithm.mapping;

import java.util.List;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.model.plan.ScheduledTransitLeg;

public class AccessibilityScore {

    public static float compute(ScheduledTransitLeg leg) {
        var fromStop = leg.getFrom().stop.getWheelchairBoarding();
        var toStop = leg.getFrom().stop.getWheelchairBoarding();
        var trip = leg.getTrip().getWheelchairBoarding();

        var values = List.of(trip, fromStop, toStop);
        var sum = (float) values.stream().mapToDouble(AccessibilityScore::accessibilityScore).sum();
        return sum / values.size();
    }

    public static double accessibilityScore(WheelChairBoarding wheelchair) {
        return switch (wheelchair) {
            case NO_INFORMATION -> 0.5;
            case POSSIBLE -> 1;
            case NOT_POSSIBLE -> 0;
        };
    }
}
