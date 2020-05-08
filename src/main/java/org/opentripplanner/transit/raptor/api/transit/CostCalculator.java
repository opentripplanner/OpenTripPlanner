package org.opentripplanner.transit.raptor.api.transit;

/**
 * The responsibility is to calculate multi-criteria value (like the generalized cost).
 * <P/>
 * The implementation should be immutable and thread safe.
 */
public interface CostCalculator {

    /**
     * Calculate a cost for boarding a transit pattern. This is used to compare the cost of
     * boarding the same pattern; So the cost must incorporate the fact that 2 boarding may
     * happen at 2 different stops.
     *
     * @param prevStopArrivalCost The cost at the previous stop arrival
     * @param boardWaitTime The time waiting before boarding at the board stop
     * @param relativeTransitTime The relative transit time to get to a (alight-) stop. It
     *                            does not matter witch point in time we measure relative to.
     *                            If one boarding happen at stop 1 and another at stop 2, then
     *                            the difference between the to relative-transit-times should
     *                            be the time measured from boarding at stop 1 until boarding
     *                            at stop 2.
     * @param boardStop The stop where the pattern is boarded
     *
     */
    int relativePatternBoardCost(
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
