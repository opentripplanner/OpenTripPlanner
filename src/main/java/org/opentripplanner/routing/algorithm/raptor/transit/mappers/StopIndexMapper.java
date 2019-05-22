package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StopIndexMapper {

    /**
     * Create map between stop and index used by Raptor to stop objects in original graph
     */
    static Map<Stop, Integer> mapIndexByStop(List<Stop> stopsByIndex) {
        Map<Stop, Integer> map = new HashMap<>();
        for(int i=0;i<stopsByIndex.size(); ++i) {
            map.put(stopsByIndex.get(i), i);
        }
        return map;
    }


    static int[] listStopIndexesForTripPattern(
            TripPattern tripPattern,
            Map<Stop, Integer> indexByStop
    ) {
        int[] stopPattern = new int[tripPattern.stopPattern.size];

        for (int i = 0; i < tripPattern.stopPattern.size; i++) {
            stopPattern[i] = indexByStop.get(tripPattern.getStop(i));
        }
        return stopPattern;
    }

}
