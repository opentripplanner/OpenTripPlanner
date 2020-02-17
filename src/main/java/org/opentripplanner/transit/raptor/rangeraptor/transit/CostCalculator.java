package org.opentripplanner.transit.raptor.rangeraptor.transit;


import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

/**
 * The responsibility for the cost calculator is to calculate the
 * multi-criteria cost.
 * <P/>
 * This class is immutable and thread safe.
 */
public class CostCalculator {
    private static final int PRECISION = 100;
    private final int minTransferCost;
    private final int boardCost;
    private final int walkFactor;
    private final int waitFactor;
    private final int transitFactor;

    /**
     * We only apply the wait factor between transits, not between access and transit;
     * Hence we start with 0 (zero) and after the first round we set this to the
     * provided {@link #waitFactor}.
     */
    private int waitFactorApplied = 0;


    CostCalculator(
            int boardCost,
            int boardSlackInSeconds,
            double walkReluctanceFactor,
            double waitReluctanceFactor,
            WorkerLifeCycle lifeCycle
    ) {
        this.boardCost = PRECISION * boardCost;
        this.walkFactor = (int) (PRECISION * walkReluctanceFactor);
        this.waitFactor = (int) (PRECISION * waitReluctanceFactor);
        this.transitFactor = PRECISION;
        this.minTransferCost = this.boardCost +  this.waitFactor * boardSlackInSeconds;
        lifeCycle.onPrepareForNextRound(this::initWaitFactor);
    }

    public int transitArrivalCost(int prevStopArrivalTime, int boardTime, int alightTime) {
        int waitTime = boardTime - prevStopArrivalTime;
        int transitTime = alightTime - boardTime;
        return waitFactorApplied * waitTime + transitFactor * transitTime + boardCost;
    }

    public int walkCost(int walkTimeInSeconds) {
        return walkFactor * walkTimeInSeconds;
    }

    public int calculateMinCost(int minTravelTime, int minNumTransfers) {
        return transitFactor * minTravelTime + minNumTransfers * minTransferCost;
    }

    private void initWaitFactor(int round) {
        // For access(round 0) and the first transit round(1) skip adding a cost for waiting,
        // we assume we can time-shift the access leg.
        this.waitFactorApplied = round < 2 ? 0 : waitFactor;
    }

    /**
     * Convert Raptor internal cost to OTP domain model cost. Inside Raptor the 1 cost unit
     * is 1/100 of a "transit second", while in the OTP domain is 1 "transit second". Cost in
     * raptor is calculated using integers ot be fast.
     */
    public static int toOtpDomainCost(int raptorCost) {
        return (int) Math.round((double) raptorCost / PRECISION);
    }
}
