package org.opentripplanner.routing.algorithm.raptor.transit_data_provider;

import com.conveyal.r5.otp2.api.transit.IntIterator;
import com.conveyal.r5.otp2.api.transit.TransferLeg;
import com.conveyal.r5.otp2.api.transit.TransitDataProvider;
import com.conveyal.r5.otp2.api.transit.TripPatternInfo;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.TripPatternForDate;
import org.opentripplanner.routing.core.TraverseModeSet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * This is the data provider for the Range Raptor search engine. It uses data from the TransitLayer, but filters it by
 * dates and modes per request. Transfers durations are pre-calculated per request based on walk speed.
 */

public class OtpRRDataProvider implements TransitDataProvider<TripSchedule> {

    private TransitLayer transitLayer;

    /** Active trip patterns by stop index */
    private List<List<TripPatternForDates>> activeTripPatternsPerStop;

    /** Transfers by stop index */
    private List<List<TransferLeg>> transfers;

    public OtpRRDataProvider(TransitLayer transitLayer, LocalDate startDate, int dayRange, TraverseModeSet transitModes,
                              double walkSpeed) {
        this.transitLayer = transitLayer;
        List<List<TripPatternForDate>> tripPatternForDates = getTripPatternsForDateRange(startDate, dayRange, transitModes);
        List<TripPatternForDates> tripPatternForDateList = MergeTripPatternForDates.merge(tripPatternForDates);
        setTripPatternsPerStop(tripPatternForDateList);
        calculateTransferDuration(walkSpeed);
    }

    /** Gets all the transfers starting at a given stop */
    @Override
    public Iterator<TransferLeg> getTransfers(int stopIndex) {
        return transfers.get(stopIndex).iterator();
    }

    /** Gets all the unique trip patterns touching a set of stops */
    @Override
    public Iterator<? extends TripPatternInfo<TripSchedule>> patternIterator(IntIterator stops) {
        Set<TripPatternInfo<TripSchedule>> activeTripPatternsForGivenStops = new HashSet<>();
        int stopIndex = stops.next();
        while (stopIndex > 0) {
            activeTripPatternsForGivenStops.addAll(activeTripPatternsPerStop.get(stopIndex));
            stopIndex = stops.next();
        }
        return activeTripPatternsForGivenStops.iterator();
    }

    @Override
    public int numberOfStops() {
        return transitLayer.getStopCount();
    }

    private List<TripPatternForDate> setActiveTripPatterns(LocalDate date, TraverseModeSet transitModes) {

        return transitLayer.getTripPatternsForDate(date).stream()
                .filter(p -> transitModes.contains(p.getTripPattern().getTransitMode()))
                .collect(toList());
    }

    private List<List<TripPatternForDate>> getTripPatternsForDateRange(LocalDate startDate, int dayRange, TraverseModeSet transitModes) {
        List<List<TripPatternForDate>> tripPatternForDates = new ArrayList<>();
        // Start at yesterdays date to account for trips that cross midnight. This is also accounted for in TripPatternForDates.
        for (LocalDate currentDate = startDate.minusDays(1); currentDate.isBefore(startDate.plusDays(dayRange)); currentDate = currentDate.plusDays(1)) {
            tripPatternForDates.add(setActiveTripPatterns(currentDate, transitModes));
        }
        return tripPatternForDates;
    }

    private void setTripPatternsPerStop(List<TripPatternForDates> tripPatternsForDate) {

        this.activeTripPatternsPerStop = Stream.generate(ArrayList<TripPatternForDates>::new)
                .limit(numberOfStops()).collect(Collectors.toList());

        for (TripPatternForDates tripPatternForDateList : tripPatternsForDate) {
            for (int i : tripPatternForDateList.getTripPattern().getStopPattern()) {
                this.activeTripPatternsPerStop.get(i).add(tripPatternForDateList);
            }
        }
    }

    private void calculateTransferDuration(double walkSpeed) {
        this.transfers = transitLayer.getTransferByStopIndex().stream()
                .map(t ->  t.stream().map(s -> new TransferWithDuration(s, walkSpeed)).collect(Collectors.<TransferLeg>toList()))
                .collect(toList());
    }
}
