package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.TransferLeg;
import org.opentripplanner.transit.raptor.api.transit.TransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.TripPatternInfo;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper.localDateForStartOfTime;

/**
 * This is the data provider for the Range Raptor search engine. It uses data from the TransitLayer, but filters it by
 * dates and modes per request. Transfers durations are pre-calculated per request based on walk speed.
 */
public class RaptorRoutingRequestTransitData implements TransitDataProvider<TripSchedule> {

    private TransitLayer transitLayer;

    /**
     * Active trip patterns by stop index
     */
    private List<List<TripPatternForDates>> activeTripPatternsPerStop;

    /**
     * Transfers by stop index
     */
    private List<List<TransferLeg>> transfers;

    private final ZonedDateTime startOfTime;

    public RaptorRoutingRequestTransitData(
            TransitLayer transitLayer,
            ZonedDateTime startOfTime,
            int dayRange,
            TraverseModeSet transitModes,
            double walkSpeed
    ) {
        this.transitLayer = transitLayer;
        this.startOfTime = startOfTime;

        LocalDate localDate = localDateForStartOfTime(startOfTime);

        List<Map<Integer, TripPatternForDate>> tripPatternForDates = getTripPatternsForDateRange(
                localDate, dayRange, transitModes
        );
        List<TripPatternForDates> tripPatternForDateList = MergeTripPatternForDates
                .merge(tripPatternForDates, startOfTime);

        setTripPatternsPerStop(tripPatternForDateList);

        calculateTransferDuration(walkSpeed);
    }

    /**
     * Gets all the transfers starting at a given stop
     */
    @Override public Iterator<TransferLeg> getTransfers(int stopIndex) {
        return transfers.get(stopIndex).iterator();
    }

    /**
     * Gets all the unique trip patterns touching a set of stops
     */
    @Override public Iterator<? extends TripPatternInfo<TripSchedule>> patternIterator(
            IntIterator stops
    ) {
        Set<TripPatternInfo<TripSchedule>> activeTripPatternsForGivenStops = new HashSet<>();
        while (stops.hasNext()) {
            activeTripPatternsForGivenStops.addAll(activeTripPatternsPerStop.get(stops.next()));
        }
        return activeTripPatternsForGivenStops.iterator();
    }

    @Override public int numberOfStops() {
        return transitLayer.getStopCount();
    }

    public ZonedDateTime getStartOfTime() {
        return startOfTime;
    }

    private Map<Integer, TripPatternForDate> listActiveTripPatterns(LocalDate date,
            TraverseModeSet transitModes) {

        return transitLayer.getTripPatternsForDate(date).stream()
                .filter(p -> transitModes.contains(p.getTripPattern().getTransitMode()))
                .collect(toMap(p -> p.getTripPattern().getId(), p -> p));
    }

    private List<Map<Integer, TripPatternForDate>> getTripPatternsForDateRange(
            LocalDate startDate,
            int dayRange,
            TraverseModeSet transitModes
    ) {
        List<Map<Integer, TripPatternForDate>> tripPatternForDates = new ArrayList<>();

        // Start at yesterdays date to account for trips that cross midnight. This is also
        // accounted for in TripPatternForDates.
        for (int d=-1; d < dayRange-1; ++d) {
            tripPatternForDates.add(listActiveTripPatterns(startDate.plusDays(d), transitModes));
        }

        return tripPatternForDates;
    }

    private void setTripPatternsPerStop(List<TripPatternForDates> tripPatternsForDate) {

        this.activeTripPatternsPerStop = Stream.generate(ArrayList<TripPatternForDates>::new)
                .limit(numberOfStops()).collect(Collectors.toList());

        for (TripPatternForDates tripPatternForDateList : tripPatternsForDate) {
            for (int i : tripPatternForDateList.getTripPattern().getStopIndexes()) {
                this.activeTripPatternsPerStop.get(i).add(tripPatternForDateList);
            }
        }
    }

    private void calculateTransferDuration(double walkSpeed) {
        this.transfers = transitLayer.getTransferByStopIndex().stream()
                .map(t -> t.stream().map(s -> new TransferWithDuration(s, walkSpeed))
                        .collect(Collectors.<TransferLeg>toList())).collect(toList());
    }
}
