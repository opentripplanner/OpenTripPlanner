package org.opentripplanner.model.plan;

import java.util.List;


/**
 * Calculate derived itinerary fields
 */
class ItinerariesCalculateLegTotals {
    long totalDurationSeconds = 0;
    long transitTimeSeconds = 0;
    int nTransitLegs = 0;
    long nonTransitTimeSeconds = 0;
    double nonTransitDistanceMeters = 0.0;
    long waitingTimeSeconds;
    boolean walkOnly = true;

    public ItinerariesCalculateLegTotals(List<Leg> legs) {
        calculate(legs);
    }

    private void calculate(List<Leg> legs) {
        long startTimeMs = legs.get(0).startTime.getTimeInMillis();
        long endTimeMs = legs.get(legs.size()-1).endTime.getTimeInMillis();

        totalDurationSeconds = Math.round((endTimeMs - startTimeMs) / 1000.0);

        for (Leg leg : legs) {
            long dt = leg.getDuration();

            if (leg.isTransitLeg()) {
                transitTimeSeconds += dt;
                ++nTransitLegs;
            }
            else if(leg.isOnStreetNonTransit()) {
                nonTransitTimeSeconds += dt;
                nonTransitDistanceMeters += leg.distanceMeters;
            }
            if(!leg.isWalkingLeg()) {
                walkOnly = false;
            }
        }
        this.waitingTimeSeconds = totalDurationSeconds
                - (transitTimeSeconds + nonTransitTimeSeconds);
    }
    int transfers() { return nTransitLegs == 0 ? 0 : nTransitLegs -1; }
}
