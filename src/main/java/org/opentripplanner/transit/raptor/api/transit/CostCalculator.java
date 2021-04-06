package org.opentripplanner.transit.raptor.api.transit;

import org.opentripplanner.transit.raptor.api.view.ArrivalView;

/**
 * The responsibility is to calculate multi-criteria value (like the generalized cost).
 * <P/>
 * The implementation should be immutable and thread safe.
 */
public interface CostCalculator<T extends RaptorTripSchedule> {

    /**
     * Calculate cost when on-board of a trip. The cost is only used to compare to paths on the
     * same trip - so any cost that is constant for a given trip can be dropped, but it will make
     * debugging easier if the cost can be compared with the "stop-arrival-cost". The cost must
     * incorporate the fact that 2 boarding may happen at 2 different stops.
     *
     * @param prevStopArrival The previous stop arrival
     * @param waitTime        The time waiting before boarding at the board stop
     * @param boardTime       The time of boarding
     */
    int onTripRidingCost(ArrivalView<T> prevStopArrival, int waitTime, int boardTime);

    /**
     * Calculate the value when arriving by transit.
     *
     * @param firstRound Indicate if this is the first round (first transit).
     */
    int transitArrivalCost(
        boolean firstRound,
        int fromStop,
        int waitTime,
        int transitTime,
        int toStop
    );

    /**
     * Calculate the value when arriving by transfer.
     */
    int walkCost(int walkTimeInSeconds);

    /**
     * Calculate the value, when waiting between the last transit and egress paths
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
