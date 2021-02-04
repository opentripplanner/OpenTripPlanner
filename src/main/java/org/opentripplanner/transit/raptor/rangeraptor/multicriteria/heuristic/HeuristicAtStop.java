package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.heuristic;

import org.opentripplanner.util.time.TimeUtils;

/**
 * Heuristic data for a given stop.
 */
public final class HeuristicAtStop {
    private final int minTravelTime;
    private final int minNumTransfers;
    private final int minCost;

    HeuristicAtStop(int minTravelTime, int minNumTransfers, int minCost) {
        this.minTravelTime = minTravelTime;
        this.minNumTransfers = minNumTransfers;
        this.minCost = minCost;
    }

    public int getMinTravelDuration() {
        return minTravelTime;
    }

    public int getMinNumTransfers() {
        return minNumTransfers;
    }

    public int getMinCost() {
        return minCost;
    }

    @Override
    public String toString() {
        return "Heuristic{" +
                "minTravelTime=" + TimeUtils.timeToStrCompact(minTravelTime) +
                ", minNumTransfers=" + minNumTransfers +
                ", minCost=" + minCost +
                '}';
    }
}
