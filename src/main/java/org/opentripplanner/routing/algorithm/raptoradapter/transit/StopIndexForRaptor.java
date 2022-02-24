package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTransferPriority;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;

/**
 * This index is temporary to help creating a fixed list of stops (by index), a reverse map
 * of indexes by stop, and to create a list of stop indexes for each trip pattern. It
 * make sure the <code>stops</code> and <code>indexByStop</code> have the same order.
 * <p>
 * Raptor uses an integer index to reference stops. This is not the stop id, but just a
 * sequence number - an index. Hence we don´t care about the order - as long as the order does
 * not change. Raptor reference stops as integers for performance reasons, it never accesses
 * stops, it does not need to. The returned itineraries from Raptor contain stop indexes, not
 * references to stops, so OTP must maintain the stop index.
 * <p>
 * The index also holds a pre-calculated board/alight cost for each stop used by Raptor during
 * routing.
 * <p>
 * The scope of instances of this class is limited to the mapping process, the final state is
 * stored in the {@link TransitLayer}.
 */
public final class StopIndexForRaptor {
    private final List<StopLocation> stopsByIndex;
    private final Map<StopLocation, Integer> indexByStop = new HashMap<>();
    public final int[] stopBoardAlightCosts;

    public StopIndexForRaptor(Collection<StopLocation> stops, TransitTuningParameters tuningParameters) {
        this.stopsByIndex = List.copyOf(stops);
        initializeIndexByStop();
        this.stopBoardAlightCosts = createStopBoardAlightCosts(stopsByIndex, tuningParameters);
    }

    public StopLocation stopByIndex(int index) {
        return stopsByIndex.get(index);
    }

    public int indexOf(StopLocation stop) {
        return indexByStop.get(stop);
    }

    public int size() {
        return stopsByIndex.size();
    }

    /**
     * Create a list of stop indexes for a given list of stops.
     */
    public int[] listStopIndexesForStops(List<StopLocation> stops) {
        int[] stopIndex = new int[stops.size()];

        for (int i = 0; i < stops.size(); i++) {
            stopIndex[i] = indexByStop.get(stops.get(i));
        }
        return stopIndex;
    }

    /**
     * Create a list of stop indexes for a given list of stops.
     */
    public int[] listStopIndexesForPattern(TripPattern pattern) {
        int[] stopIndex = new int[pattern.numberOfStops()];

        for (int i = 0; i < pattern.numberOfStops(); i++) {
            stopIndex[i] = indexByStop.get(pattern.getStop(i));
        }
        return stopIndex;
    }

    /**
     * Create map between stop and index used by Raptor to stop objects in original graph
     */
    private void initializeIndexByStop() {
        for(int i = 0; i< stopsByIndex.size(); ++i) {
            indexByStop.put(stopsByIndex.get(i), i);
        }
    }

    /**
     * Create static board/alight cost for Raptor to include for each stop.
     */
    private static int[] createStopBoardAlightCosts(
        List<StopLocation> stops,
        TransitTuningParameters tuningParams
    ) {
        if(!tuningParams.enableStopTransferPriority()) {
            return null;
        }
        int[] stopVisitCosts = new int[stops.size()];

        for (int i=0; i<stops.size(); ++i) {
            StopTransferPriority priority = stops.get(i).getPriority();
            int domainCost = tuningParams.stopTransferCost(priority);
            stopVisitCosts[i] = RaptorCostConverter.toRaptorCost(domainCost);
        }
        return stopVisitCosts;
    }
}
