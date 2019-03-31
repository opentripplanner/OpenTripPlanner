package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import org.opentripplanner.model.Stop;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TransitLayer {

    /** Transit data required for routing */
    Map<LocalDate, List<TripPatternForDate>> tripPatternsForDate;
    /** Index of outer list is from stop index, inner list index has no specific meaning. To stop index is a field of the
     * Transfer object.
     */
    List<List<Transfer>> transferByStopIndex;

    /** Maps to original graph to retrieve additional data */
    List<Stop> stopsByIndex;
    Map<Stop, Integer> indexByStop;

    public int getStopCount() { return stopsByIndex.size(); }

    public int getIndexByStop(Stop stop) {
        return indexByStop.get(stop);
    }

    public Stop getStopByIndex(int stopIndex) {
        return stopIndex != -1 ? stopsByIndex.get(stopIndex) : null;
    }

    public Collection<TripPatternForDate> getTripPatternsForDate(LocalDate date) {
        return tripPatternsForDate.get(date);
    }

    public List<List<Transfer>> getTransferByStopIndex() {
        return this.transferByStopIndex;
    }
}
