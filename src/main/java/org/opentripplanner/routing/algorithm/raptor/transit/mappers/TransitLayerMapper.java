package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
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
import java.util.stream.Collectors;

import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.StopIndexMapper.listStopIndexesForTripPattern;
import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.StopIndexMapper.mapIndexByStop;
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


    private TransitLayerMapper(Graph graph) {
        this.graph = graph;
    }

    public static TransitLayer map(Graph graph) {
        return new TransitLayerMapper(graph).map();
    }

    private TransitLayer map() {
        List<Stop> stopsByIndex;
        Map<Stop, Integer> indexByStop;
        HashMap<LocalDate, List<TripPatternForDate>> tripPatternsByStopByDate;
        List<List<Transfer>> transferByStopIndex;

        LOG.info("Mapping transitLayer from Graph...");

        stopsByIndex = mapStopsByIndex();
        indexByStop = mapIndexByStop(stopsByIndex);
        tripPatternsByStopByDate = mapTripPatterns(indexByStop);
        transferByStopIndex = mapTransfers(graph.index.stopVertexForStop, stopsByIndex, indexByStop);

        LOG.info("Mapping complete.");

        return new TransitLayer(tripPatternsByStopByDate, transferByStopIndex, stopsByIndex, indexByStop);
    }

    private ArrayList<Stop> mapStopsByIndex() {
        return new ArrayList<>(this.graph.index.stopForId.values());
    }

    /**
     * Map pre-Raptor TripPatterns and Trips to the corresponding Raptor classes.
     */
    private HashMap<LocalDate, List<TripPatternForDate>> mapTripPatterns (Map<Stop, Integer> indexByStop) {
        // CalendarService has one main implementation (CalendarServiceImpl) which contains a CalendarServiceData which
        // can easily supply all of the dates. But it's impossible to actually see those dates without modifying the
        // interfaces and inheritance. So we have to work around this abstraction and reconstruct the CalendarData.
        // Note the "multiCalendarServiceImpl" which has docs saying it expects one single CalendarData.
        // It seems to merge the calendar services from multiple GTFS feeds, but its only documentation says it's a hack.

        // Reconstruct set of all dates where service is defined, keeping track of which services run on which days.
        CalendarService calendarService = graph.getCalendarService();
        Multimap<ServiceDate, FeedScopedId> serviceIdsForServiceDate = HashMultimap.create();
        for (FeedScopedId serviceId : calendarService.getServiceIds()) {
            Set<ServiceDate> serviceDatesForService = calendarService.getServiceDatesForServiceId(serviceId);
            for (ServiceDate serviceDate : serviceDatesForService) {
                serviceIdsForServiceDate.put(serviceDate, serviceId);
            }
        }

        // Convert all old TripPatterns into new ones, keeping a Map between the two.
        // Do this conversion up front (rather than lazily on demand) to ensure pattern IDs match the sequence of
        // patterns in source data.
        Map<org.opentripplanner.routing.edgetype.TripPattern, TripPattern> newTripPatternForOld = new HashMap<>();
        int patternId = 0;
        for (org.opentripplanner.routing.edgetype.TripPattern oldTripPattern : graph.tripPatternForId.values()) {
            TripPattern newTripPattern = new TripPattern(
                    patternId++,
                    // TripPatternForDate should never access the tripTimes inside the TripPattern, so I've left them null.
                    // No TripSchedules in the pattern itself; put them in the TripPatternForDate
                    null,
                    oldTripPattern.mode,
                    listStopIndexesForTripPattern(oldTripPattern, indexByStop)
            );
            newTripPatternForOld.put(oldTripPattern, newTripPattern);
        }

        // Presumably even when applying realtime, many TripTimes will recur on future dates.
        // This Map is used to deduplicate the resulting TripSchedules.
        Map<TripTimes, TripSchedule> tripScheduleForTripTimes = new HashMap<>();

        // The return value of this entire process.
        HashMap<LocalDate, List<TripPatternForDate>> tripPatternsForDates = new HashMap<>();

        // Loop over all dates for which any service is defined
        // We need the original TripPattern to look it up in the timetable snapshot, but also need to convert it to a
        // new Raptor TripPattern.
        // Get a frozen snapshot of timetable data so it isn't changed by incoming realtime data while we're iterating.
        TimetableSnapshot timetableSnapshot = null;
        if (graph.timetableSnapshotSource == null) {
            LOG.info("There is no timetable snapshot source. This TransitLayer will reflect scheduled service.");
        } else {
            timetableSnapshot = graph.timetableSnapshotSource.getTimetableSnapshot();
            LOG.info("Got timetable snapshot. This TransitLayer will reflect realtime updates.");
        }
        Set<ServiceDate> allServiceDates = serviceIdsForServiceDate.keySet();
        for (ServiceDate serviceDate : allServiceDates) {
            // Create LocalDate equivalent to the OTP/GTFS ServiceDate object, serving as the key of the return Map.
            LocalDate localDate = ServiceCalendarMapper.localDateFromServiceDate(serviceDate);
            // Create a List to hold the values for one entry in the return Map.
            List<TripPatternForDate> values = new ArrayList<>();
            // This nested loop could be quite inefficient.
            // Maybe determine in advance which patterns are running on each service and day.
            TIntSet serviceCodesRunning = new TIntHashSet();
            for (FeedScopedId serviceId : serviceIdsForServiceDate.get(serviceDate)) {
                serviceCodesRunning.add(graph.serviceCodes.get(serviceId));
            }
            for (org.opentripplanner.routing.edgetype.TripPattern oldTripPattern : graph.tripPatternForId.values()) {
                // Get an updated or scheduled timetable depending on the date.
                // This might have the trips pre-filtered for the specified date, that needs to be investigated.
                // But in any case we might end up with a scheduled timetable, which can include non-running trips.
                // So filter the trips according to which service IDs are running on the given day.
                Timetable timetable = oldTripPattern.scheduledTimetable;
                if (timetableSnapshot != null) {
                    timetable = timetableSnapshot.resolve(oldTripPattern, serviceDate);
                }
                List<TripSchedule> newTripSchedules = new ArrayList<>();
                // The TripTimes are not sorted by departure time in the source timetable. Results are wrong unless trips are sorted.
                for (TripTimes tripTimes : getSortedTripTimes(timetable)) {
                    if (!serviceCodesRunning.contains(tripTimes.serviceCode)) {
                        continue;
                    }
                    TripSchedule tripSchedule = tripScheduleForTripTimes.computeIfAbsent(tripTimes,
                            tt -> TripScheduleMapper.map(oldTripPattern, tt));
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

    // TODO About 80% of the mapping time is spent in this method. Should be consider pre-sorting these before
    // TODO serializing the graph?
    private List<TripTimes> getSortedTripTimes (Timetable timetable) {
        return timetable.tripTimes.stream()
                .sorted(Comparator.comparing(t -> t.getArrivalTime(0)))
                .collect(Collectors.toList());
    }

    private Collection<org.opentripplanner.routing.edgetype.TripPattern> originalTripPatterns() {
        return graph.tripPatternForId.values();
    }
}
