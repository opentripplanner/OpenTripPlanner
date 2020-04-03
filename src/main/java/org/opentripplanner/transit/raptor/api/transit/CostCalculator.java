package org.opentripplanner.transit.raptor.api.transit;

/**
 * The responsibility is to calculate multi-criteria value (like the generalized cost).
 * <P/>
 * The implementation should be immutable and thread safe.
 */
public interface CostCalculator {

    /**
     * Calculate the value when arriving by transit.
     */
    int transitArrivalCost(int waitTime, int transitTime);

    /**
     * Calculate the value when arriving by transfer.
     */
    int walkCost(int walkTimeInSeconds);

    /**
     * Used for estimating the remaining value for a criteria at a given stop arrival. The
     * calculated value should be a an optimistic estimate for the heuristics to work properly. So,
     * to calculate the generalized cost for given the {@code minTravelTime} and {@code
     * minNumTransfers} retuning the greatest value, witch is guaranteed to be less than the
     * <em>real value</em> would be correct and a good choose.
     */
    int calculateMinCost(int minTravelTime, int minNumTransfers);

    /**
     * Convert Raptor internal cost to OTP domain model cost. Inside Raptor the 1 cost unit
     * is 1/100 of a "transit second", while in the OTP domain is 1 "transit second". Cost in
     * raptor is calculated using integers ot be fast.
     */
    int toOtpDomainCost(int raptorCost);
}
