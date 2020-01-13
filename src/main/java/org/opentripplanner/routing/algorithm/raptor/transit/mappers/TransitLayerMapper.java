package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.TripScheduleWrapperImpl;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransfersMapper.mapTransfers;

/**
 * Maps the TransitLayer object from the OTP Graph object. The ServiceDay hierarchy is reversed,
 * with service days at the top level, which contains TripPatternForDate objects that contain
 * only TripSchedules running on that particular date. This makes it faster to filter out
 * TripSchedules when doing Range Raptor searches.
 */
public class TransitLayerMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayerMapper.class);

    private final Graph graph;

    private final TimetableSnapshot timetableSnapshot;

    private TransitLayerMapper(Graph graph) {
        this.graph = graph;
        this.timetableSnapshot = graph.getTimetableSnapshot();
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
            stopIndex.stops,
            stopIndex.indexByStop,
            graph.getTimeZone().toZoneId()
        );
    }

    /**
     * Map pre-Raptor TripPatterns and Trips to the corresponding Raptor classes.
     *
     * TODO OTP2 - This can be refactored and broken up into smaller. Se discussion in PR #2794
     */
    private HashMap<LocalDate, List<TripPatternForDate>> mapTripPatterns (StopIndexForRaptor stopIndex) {
        // CalendarService has one main implementation (CalendarServiceImpl) which contains a
        // CalendarServiceData which can easily supply all of the dates. But it's impossible to
        // actually see those dates without modifying the interfaces and inheritance. So we have
        // to work around this abstraction and reconstruct the CalendarData.
        // Note the "multiCalendarServiceImpl" which has docs saying it expects one single
        // CalendarData. It seems to merge the calendar services from multiple GTFS feeds, but
        // its only documentation says it's a hack.
        // TODO OTP2 - This cleanup is added to the 'Final cleanup OTP2' issue #2757

        // Reconstruct set of all dates where service is defined, keeping track of which services
        // run on which days.
        CalendarService calendarService = graph.getCalendarService();
        Multimap<ServiceDate, FeedScopedId> serviceIdsForServiceDate = HashMultimap.create();

        for (FeedScopedId serviceId : calendarService.getServiceIds()) {
            Set<ServiceDate> serviceDatesForService = calendarService.getServiceDatesForServiceId(serviceId);
            for (ServiceDate serviceDate : serviceDatesForService) {
                serviceIdsForServiceDate.put(serviceDate, serviceId);
            }
        }

        // If we are using realtime updates, we want to include both the TripPatterns in the scheduled (static) data
        // and any new patterns that were created by realtime data (added or rerouted trips).
        // So far, realtime messages cannot add new stops or service IDs, so we can use those straight from the Graph.
        Collection<org.opentripplanner.model.TripPattern> allTripPatterns;
        allTripPatterns = graph.tripPatternForId.values();
        if (timetableSnapshot != null && !timetableSnapshot.getAllRealtimeTripPatterns().isEmpty()) {
            // Make a protective copy, also removing duplicates between the scheduled and updated patterns.
            // This may be somewhat inefficient since we copy all patterns into a Set even when none have been added.
            // However references to the TripPatternCache are private, and none is held by the timetableSnapshot so
            // this is simpler.
            allTripPatterns = new HashSet(allTripPatterns);
            allTripPatterns.addAll(timetableSnapshot.getAllRealtimeTripPatterns());
        }

        final Map<org.opentripplanner.model.TripPattern, TripPattern> newTripPatternForOld;
        newTripPatternForOld = mapOldTripPatternToRaptorTripPattern(stopIndex, allTripPatterns);

        // Presumably even when applying realtime, many TripTimes will recur on future dates.
        // This Map is used to deduplicate the resulting TripSchedules.
        Map<TripTimes, TripSchedule> tripScheduleForTripTimes = new HashMap<>();

        // The return value of this entire process.
        HashMap<LocalDate, List<TripPatternForDate>> tripPatternsForDates = new HashMap<>();

        // Loop over all dates for which any service is defined. We need the original TripPattern to look it up in the
        // timetable snapshot, but also need to convert it to a new Raptor TripPattern. Get a frozen snapshot of
        // timetable data so it isn't changed by incoming realtime data while we're iterating.
        if (timetableSnapshot == null) {
            LOG.info("This TransitLayerMapper could not get a realtime timetable snapshot. The TransitLayer will reflect only scheduled service.");
        } else {
            LOG.info("This TransitLayerMapper got a realtime timetable snapshot. The TransitLayer will reflect realtime updates to scheduled service.");
        }
        Set<ServiceDate> allServiceDates = serviceIdsForServiceDate.keySet();
        Map<Timetable, List<TripTimes>> sortedTripTimesForTimetable = new HashMap<>();
        for (ServiceDate serviceDate : allServiceDates) {
            // Create LocalDate equivalent to the OTP/GTFS ServiceDate object, serving as the key of
            // the return Map.
            LocalDate localDate = ServiceCalendarMapper.localDateFromServiceDate(serviceDate);

            // Create a List to hold the values for one entry in the return Map.
            List<TripPatternForDate> values = new ArrayList<>();
            TIntSet serviceCodesRunning = new TIntHashSet();

            for (FeedScopedId serviceId : serviceIdsForServiceDate.get(serviceDate)) {
                serviceCodesRunning.add(graph.serviceCodes.get(serviceId));
            }

            // This nested loop could be quite inefficient.
            // Maybe determine in advance which patterns are running on each service and day.
            for (org.opentripplanner.model.TripPattern oldTripPattern : allTripPatterns) {
                // Get an updated or scheduled timetable depending on the date. This might have the
                // trips pre-filtered for the specified date, that needs to be investigated. But in
                // any case we might end up with a scheduled timetable, which can include
                // non-running trips. So filter the trips according to which service IDs are running
                // on the given day.
                Timetable timetable = oldTripPattern.scheduledTimetable;
                if (timetableSnapshot != null) {
                    timetable = timetableSnapshot.resolve(oldTripPattern, serviceDate);
                }
                List<TripSchedule> newTripSchedules = new ArrayList<>();
                // The TripTimes are not sorted by departure time in the source timetable because
                // OTP1 performs a simple/ linear search. Raptor results depend on trips being
                // sorted. We reuse the same timetables many times on different days, so cache the
                // sorted versions to avoid repeated compute-intensive sorting. Anecdotally this
                // reduces mapping time by more than half, but it is still rather slow. NL Mapping
                // takes 32 seconds sorting every timetable, 9 seconds with cached sorting, and 6
                // seconds with no timetable sorting at all.
                List<TripTimes> sortedTripTimes = sortedTripTimesForTimetable.computeIfAbsent(
                    timetable,
                    TransitLayerMapper::getSortedTripTimes
                );
                for (TripTimes tripTimes : sortedTripTimes) {
                    if (!serviceCodesRunning.contains(tripTimes.serviceCode)) {
                        continue;
                    }
                    if (tripTimes.getRealTimeState() == RealTimeState.CANCELED) {
                        continue;
                    }
                    TripSchedule tripSchedule = tripScheduleForTripTimes.computeIfAbsent(
                        tripTimes,
                        // The following are two alternative implementations of TripSchedule
                        tt -> new TripScheduleWrapperImpl(tt, oldTripPattern)
                        // tt -> tt.toTripSchedulImpl(oldTripPattern)
                    );
                    newTripSchedules.add(tripSchedule);
                } 
                TripPattern newTripPattern = newTripPatternForOld.get(oldTripPattern);
                TripPatternForDate tripPatternForDate = new TripPatternForDate(
                        newTripPattern,
                        newTripSchedules,
                        localDate
                );
                values.add(tripPatternForDate);
            }
            tripPatternsForDates.put(localDate, values);
        }
        return tripPatternsForDates;
    }

    /**
     * Convert all old TripPatterns into new ones, keeping a Map between the two.
     * Do this conversion up front (rather than lazily on demand) to ensure pattern IDs match
     * the sequence of patterns in source data.
     */
    private static Map<org.opentripplanner.model.TripPattern, TripPattern> mapOldTripPatternToRaptorTripPattern(
            StopIndexForRaptor stopIndex,
            Collection<org.opentripplanner.model.TripPattern> oldTripPatterns
    ) {
        Map<org.opentripplanner.model.TripPattern, TripPattern> newTripPatternForOld;
        newTripPatternForOld = new HashMap<>();
        int patternId = 0;

        for (org.opentripplanner.model.TripPattern oldTripPattern : oldTripPatterns) {
            TripPattern newTripPattern = new TripPattern(
                    patternId++,
                    // TripPatternForDate should never access the tripTimes inside the TripPattern,
                    // so I've left them null.
                    // No TripSchedules in the pattern itself; put them in the TripPatternForDate
                    null,
                    oldTripPattern.mode,
                    stopIndex.listStopIndexesForStops(oldTripPattern.stopPattern.stops)
            );
            newTripPatternForOld.put(oldTripPattern, newTripPattern);
        }
        return newTripPatternForOld;
    }

    // TODO About 80% of the mapping time is spent in this method. Should be consider pre-sorting these before
    // TODO serializing the graph?
    private static List<TripTimes> getSortedTripTimes (Timetable timetable) {
        return timetable.tripTimes.stream()
                .sorted(Comparator.comparing(t -> t.getArrivalTime(0)))
                .collect(Collectors.toList());
    }
}
