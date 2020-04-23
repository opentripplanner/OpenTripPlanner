package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransfersMapper.mapTransfers;
import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.TripPatternMapper.mapOldTripPatternToRaptorTripPattern;

/**
 * Maps the TransitLayer object from the OTP Graph object. The ServiceDay hierarchy is reversed,
 * with service days at the top level, which contains TripPatternForDate objects that contain
 * only TripSchedules running on that particular date. This makes it faster to filter out
 * TripSchedules when doing Range Raptor searches.
 *
 * CONCURRENCY: This mapper run part of the mapping in parallel using parallel streams. This
 *              improve startup time on the Norwegian graph by 20 seconds; reducing the this
 *              mapper from 36 seconds to 15 seconds, and the total startup time from 80 seconds
 *              to 60 seconds. (JAN 2020, MacBook Pro, 3.1 GHz i7)
 */
public class TransitLayerMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayerMapper.class);

    private final Graph graph;

    private TransitLayerMapper(Graph graph) {
        this.graph = graph;
    }

    public static TransitLayer map(TransitTuningParameters tuningParameters, Graph graph) {
        return new TransitLayerMapper(graph).map(tuningParameters);
    }

    private TransitLayer map(TransitTuningParameters tuningParameters) {
        StopIndexForRaptor stopIndex;
        HashMap<LocalDate, List<TripPatternForDate>> tripPatternsByStopByDate;
        List<List<Transfer>> transferByStopIndex;

        LOG.info("Mapping transitLayer from Graph...");

        stopIndex =  new StopIndexForRaptor(graph.index.getAllStops(), tuningParameters);
        tripPatternsByStopByDate = mapTripPatterns(stopIndex);
        transferByStopIndex = mapTransfers(stopIndex, graph.transfersByStop);

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
     * <p>
     * Part of this method runs IN PARALLEL.
     * <p>
     */
    private HashMap<LocalDate, List<TripPatternForDate>> mapTripPatterns (
        StopIndexForRaptor stopIndex
    ) {
        Collection<TripPattern> allTripPatterns = graph.tripPatternForId.values();

        final Map<TripPattern, TripPatternWithRaptorStopIndexes> newTripPatternForOld =
            mapOldTripPatternToRaptorTripPattern(stopIndex, allTripPatterns);


        TripPatternForDateMapper tripPatternForDateMapper = new TripPatternForDateMapper(
            graph.index.getServiceCodesRunningForDate(),
            newTripPatternForOld
        );

        Set<ServiceDate> allServiceDates = graph.index.getServiceCodesRunningForDate().keySet();

        // The return value of this entire process.
        ConcurrentHashMap<LocalDate, List<TripPatternForDate>> result = new ConcurrentHashMap<>();


        // THIS CODE RUNS IN PARALLEL
        allServiceDates
            .parallelStream()
            .forEach(serviceDate -> {
                // Create LocalDate equivalent to the OTP/GTFS ServiceDate object, serving as the key of
                // the return Map.
                LocalDate localDate = ServiceCalendarMapper.localDateFromServiceDate(serviceDate);

                // Create a List to hold the values for one entry in the return Map.
                List<TripPatternForDate> values = new ArrayList<>();

                // This nested loop could be quite inefficient.
                // Maybe determine in advance which patterns are running on each service and day.
                for (org.opentripplanner.model.TripPattern oldTripPattern : allTripPatterns) {
                    TripPatternForDate tripPatternForDate =
                        tripPatternForDateMapper.map(
                            oldTripPattern.scheduledTimetable,
                            serviceDate
                    );
                    if (tripPatternForDate != null) {
                        values.add(tripPatternForDate);
                    }
                }
                if (!values.isEmpty()) {
                    result.put(localDate, values);
                }
            });
        // END PARALLEL CODE

        return new HashMap<>(result);
    }

    // TODO We can save time by either pre-sorting these or use a sorting algorithm that is
    //      optimized for sorting nearly sorted list
    static List<TripTimes> getSortedTripTimes (Timetable timetable) {
        return timetable.tripTimes.stream()
                .sorted(Comparator.comparing(t -> t.getArrivalTime(0)))
                .collect(Collectors.toList());
    }
}
