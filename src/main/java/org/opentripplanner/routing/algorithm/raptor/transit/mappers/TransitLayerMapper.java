package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransfersMapper.mapTransfers;
import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.TripPatternMapper.mapOldTripPatternToRaptorTripPattern;

/**
 * Maps the TransitLayer object from the OTP Graph object. The ServiceDay hierarchy is reversed,
 * with service days at the top level, which contains TripPatternForDate objects that contain
 * only TripSchedules running on that particular date. This makes it faster to filter out
 * TripSchedules when doing Range Raptor searches.
 */
public class TransitLayerMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayerMapper.class);

    private final Graph graph;

    private TransitLayerMapper(Graph graph) {
        this.graph = graph;
    }

    public static TransitLayer map(Graph graph) {
        return new TransitLayerMapper(graph).map();
    }

    private TransitLayer map() {
        StopIndexForRaptor stopIndex;
        HashMap<LocalDate, List<TripPatternForDate>> tripPatternsByStopByDate;
        List<List<Transfer>> transferByStopIndex;

        LOG.info("Mapping transitLayer from Graph...");

        stopIndex =  new StopIndexForRaptor(graph.index.stopForId.values());
        tripPatternsByStopByDate = mapTripPatterns(stopIndex);
        transferByStopIndex = mapTransfers(graph.index.stopVertexForStop, stopIndex);

        LOG.info("Mapping complete.");

        return new TransitLayer(
            tripPatternsByStopByDate,
            transferByStopIndex,
            stopIndex,
            graph.getTimeZone().toZoneId()
        );
    }

    /**
     * Map pre-Raptor TripPatterns and Trips to the corresponding Raptor classes.
     *
     * TODO OTP2 - This can be refactored and broken up into smaller. Se discussion in PR #2794
     */
    private HashMap<LocalDate, List<TripPatternForDate>> mapTripPatterns (StopIndexForRaptor stopIndex) {

        // If we are using realtime updates, we want to include both the TripPatterns in the scheduled (static) data
        // and any new patterns that were created by realtime data (added or rerouted trips).
        // So far, realtime messages cannot add new stops or service IDs, so we can use those straight from the Graph.
        Collection<org.opentripplanner.model.TripPattern> allTripPatterns;
        allTripPatterns = graph.tripPatternForId.values();

        final Map<org.opentripplanner.model.TripPattern, TripPattern>
        newTripPatternForOld =
            mapOldTripPatternToRaptorTripPattern(stopIndex, allTripPatterns);

        // The return value of this entire process.
        HashMap<LocalDate, List<TripPatternForDate>> tripPatternsForDates = new HashMap<>();

        TripPatternForDateMapper tripPatternForDateMapper = new TripPatternForDateMapper(
            graph.index.getServiceCodesRunningForDate(),
            newTripPatternForOld
        );

        for (ServiceDate serviceDate : graph.index.getServiceCodesRunningForDate().keySet()) {
            // Create LocalDate equivalent to the OTP/GTFS ServiceDate object, serving as the key of
            // the return Map.
            LocalDate localDate = ServiceCalendarMapper.localDateFromServiceDate(serviceDate);

            // Create a List to hold the values for one entry in the return Map.
            List<TripPatternForDate> values = new ArrayList<>();

            // This nested loop could be quite inefficient.
            // Maybe determine in advance which patterns are running on each service and day.
            for (org.opentripplanner.model.TripPattern oldTripPattern : allTripPatterns) {
                TripPatternForDate tripPatternForDate = tripPatternForDateMapper.map(oldTripPattern.scheduledTimetable, serviceDate);
                if (tripPatternForDate != null) {
                    values.add(tripPatternForDate);
                }
            }
            tripPatternsForDates.put(localDate, values);
        }
        return tripPatternsForDates;
    }



    // TODO We can save time by either pre-sorting these or use a sorting algorithm that is
    //      optimized for sorting nearly sorted list
    static List<TripTimes> getSortedTripTimes (Timetable timetable) {
        return timetable.tripTimes.stream()
                .sorted(Comparator.comparing(t -> t.getArrivalTime(0)))
                .collect(Collectors.toList());
    }
}
