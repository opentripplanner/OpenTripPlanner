package org.opentripplanner.transit.raptor.api.transit;

/**
 * The responsibility is to calculate multi-criteria value (like the generalized cost).
 * <P/>
 * The implementation should be immutable and thread safe.
 */
public interface CostCalculator {

    /**
     * Calculate cost when on-board of a trip. The cost is only used to compare to paths on the
     * same trip - so any cost that is constant for a given trip can be dropped, but it will make
     * debugging easier if the cost can be compared with the "stop-arrival-cost". The cost must
     * incorporate the fact that 2 boarding may happen at 2 different stops.
     *
     * @param prevStopArrivalCost The cost at the previous stop arrival
     * @param boardWaitTime The time waiting before boarding at the board stop
     * @param relativeTransitTime The relative transit time. This should increase for each stop
     *                           visited in the pattern with the same amount of seconds that it
     *                           takes to travel from the previous stop to the next. The
     *                           'relativeTransitTime' for the fisrt stop can be any number, even
     *                           negative.
     * @param boardStop The stop where the pattern is boarded
     *
     */
    int onTripRidingCost(
        int prevStopArrivalCost, int boardWaitTime, int relativeTransitTime, int boardStop
    );

    /**
     * Calculate the value when arriving by transit.
     */
    int transitArrivalCost(int waitTime, int transitTime, int fromStop, int toStop);

    /**
     * Calculate the value when arriving by transfer.
     */
    int walkCost(int walkTimeInSeconds);

    /**
     * Calculate the value, when waiting between the last transit and egress legs
     */
    int waitCost(int waitTimeInSeconds);

    /**
     * Used for estimating the remaining value for a criteria at a given stop arrival. The
     * calculated value should be a an optimistic estimate for the heuristics to work properly. So,
     * to calculate the generalized cost for given the {@code minTravelTime} and {@code
     * minNumTransfers} retuning the greatest value, witch is guaranteed to be less than the
     * <em>real value</em> would be correct and a good choose.
     */
    int calculateMinCost(int minTravelTime, int minNumTransfers);
}
