package org.opentripplanner.transit.raptor.rangeraptor.transit;


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


    CostCalculator(
            int boardCost,
            int boardSlackInSeconds,
            double walkReluctanceFactor,
            double waitReluctanceFactor
    ) {
        this.boardCost = PRECISION * boardCost;
        this.walkFactor = (int) (PRECISION * walkReluctanceFactor);
        this.waitFactor = (int) (PRECISION * waitReluctanceFactor);
        this.transitFactor = PRECISION;
        this.minTransferCost = this.boardCost +  this.waitFactor * boardSlackInSeconds;
    }


    public int transitArrivalCost(int prevStopArrivalTime, int boardTime, int alightTime) {
        int waitTime = boardTime - prevStopArrivalTime;
        int transitTime = alightTime - boardTime;
        return waitFactor * waitTime + transitFactor * transitTime + boardCost;
    }

    public int walkCost(int walkTimeInSeconds) {
        return walkFactor * walkTimeInSeconds;
    }

    public int calculateMinCost(int minTravelTime, int minNumTransfers) {
        return transitFactor * minTravelTime + minNumTransfers * minTransferCost;
    }
}
