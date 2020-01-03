package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.Stop;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransitLayer {

    /**
     * Transit data required for routing
     */
    private final HashMap<LocalDate, List<TripPatternForDate>> tripPatternsForDate;

    /**
     * Index of outer list is from stop index, inner list index has no specific meaning.
     * To stop index is a field of the Transfer object.
     */
    private final List<List<Transfer>> transferByStopIndex;

    /**
     * Maps to original graph to retrieve additional data
     */
    private final StopIndexForRaptor stopIndex;

    public TransitLayer(
        Map<LocalDate, List<TripPatternForDate>> tripPatternsForDate,
        List<List<Transfer>> transferByStopIndex,
        StopIndexForRaptor stopIndex
    ) {
        this.tripPatternsForDate = new HashMap<>(tripPatternsForDate);
        this.transferByStopIndex = transferByStopIndex;
        this.stopIndex = stopIndex;
    }

    public int getStopCount() {
        return stopIndex.stops.size();
    }

    public int getIndexByStop(Stop stop) {
        return stopIndex.indexByStop.get(stop);
    }

    public Stop getStopByIndex(int index) {
        return index != -1 ? stopIndex.stops.get(index) : null;
    }

    public StopIndexForRaptor getStopIndex() {
        return this.stopIndex;
    }

    public List<List<Transfer>> getTransferByStopIndex() {
        return this.transferByStopIndex;
    }

    public List<TripPatternForDate> getTripPatternsForDateCopy(LocalDate date) {
        List<TripPatternForDate> tripPatternForDate = tripPatternsForDate.get(date);
        return tripPatternForDate != null ? new ArrayList<>(tripPatternsForDate.get(date)) : null;
    }

    public Collection<TripPatternForDate> getTripPatternsForDate(LocalDate date) {
        return tripPatternsForDate.getOrDefault(date, Collections.emptyList());
    }

    /**
     * Replaces all the TripPatternForDates for a single date. This is an atomic operation according
     * to the HashMap implementation.
     */
    public void replaceTripPatternsForDate(
        LocalDate date,
        List<TripPatternForDate> tripPatternForDates
    ) {
        this.tripPatternsForDate.replace(date, tripPatternForDates);
    }
}
