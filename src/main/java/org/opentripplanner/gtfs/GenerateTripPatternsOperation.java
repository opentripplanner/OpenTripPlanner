package org.opentripplanner.gtfs;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.GTFSModeNotSupported;
import org.opentripplanner.graph_builder.issues.TripDegenerate;
import org.opentripplanner.graph_builder.issues.TripUndefinedService;
import org.opentripplanner.model.Direction;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for generating trip patterns when loading GTFS data.
 */
public class GenerateTripPatternsOperation {
    private static final Logger LOG = LoggerFactory.getLogger(GenerateTripPatternsOperation.class);

    private final Map<String, Integer> tripPatternIdCounters = new HashMap<>();

    private final OtpTransitServiceBuilder transitDaoBuilder;
    private final DataImportIssueStore issueStore;
    private final Deduplicator deduplicator;
    private final Set<FeedScopedId> calendarServiceIds;

    private final Multimap<StopPattern, TripPattern> tripPatterns;
    private final Map<Trip, List<Frequency>> frequenciesForTrip;

    private int tripCount = 0;
    private int freqCount = 0;
    private int scheduledCount = 0;



    public GenerateTripPatternsOperation(OtpTransitServiceBuilder builder, DataImportIssueStore issueStore,
            Deduplicator deduplicator, Set<FeedScopedId> calendarServiceIds) {
        this.transitDaoBuilder = builder;
        this.issueStore = issueStore;
        this.deduplicator = deduplicator;
        this.calendarServiceIds = calendarServiceIds;
        this.tripPatterns = transitDaoBuilder.getTripPatterns();
        this.frequenciesForTrip = transitDaoBuilder.getFrequencies()
                .stream()
                .collect(Collectors.groupingBy(Frequency::getTrip));
    }

    public void run() {
        final Collection<Trip> trips = transitDaoBuilder.getTripsById().values();
        final int tripsSize = trips.size();

        /* Loop over all trips, handling each one as a frequency-based or scheduled trip. */
        for (Trip trip : trips) {
            if (++tripCount % 100000 == 0) {
                LOG.debug("build trip patterns {}/{}", tripCount, tripsSize);
            }

            buildTripPatternForTrip(trip);
        }

        LOG.info("Added {} frequency-based and {} single-trip timetable entries.", freqCount,
                scheduledCount);
    }

    public boolean hasFrequencyBasedTrips() {
        return freqCount > 0;
    }

    public boolean hasScheduledTrips() {
        return scheduledCount > 0;
    }

    private void buildTripPatternForTrip(Trip trip) {
        // TODO: move to a validator module
        // Check that the mode is supported
        if(trip.getRoute().getMode() == null) {
            issueStore.add(new GTFSModeNotSupported(trip, Integer.toString(trip.getRoute().getType())));
            return;
        }

        // TODO: move to a validator module
        if (!calendarServiceIds.contains(trip.getServiceId())) {
            issueStore.add(new TripUndefinedService(trip));
            return; // Invalid trip, skip it, it will break later
        }

        Collection<StopTime> stopTimes = transitDaoBuilder.getStopTimesSortedByTrip().get(trip);

        // If after filtering this trip does not contain at least 2 stoptimes, it does not serve any purpose.
        if (stopTimes.size() < 2) {
            issueStore.add(new TripDegenerate(trip));
            return;
        }

        // Get the existing TripPattern for this filtered StopPattern, or create one.
        StopPattern stopPattern = new StopPattern(stopTimes);

        Direction direction = trip.getDirection();
        TripPattern tripPattern = findOrCreateTripPattern(
            stopPattern, trip.getRoute(), direction
        );

        // Create a TripTimes object for this list of stoptimes, which form one trip.
        TripTimes tripTimes = new TripTimes(trip, stopTimes, deduplicator);

        var frequencies = frequenciesForTrip.get(trip);
        // If trip was not frequency-based, add TripTimes directly to the TripPattern's scheduled
        // timetable.
        if(frequencies == null) {
            tripPattern.add(tripTimes);
            scheduledCount++;
        }
        // If trip is referenced by one or more lines in frequencies.txt, then expand the
        // trip-pattern with new TripTimes for each depature
        else {
            for (Frequency freq : frequencies) {
                int timeShift = timeShiftForFrequencyBasedTrip(tripTimes, freq);
                while (timeShift < freq.getEndTime()) {
                    tripPattern.add(new TripTimes(tripTimes, freq, timeShift));
                    timeShift += freq.getHeadwaySecs();
                }
                freqCount++;
            }
            // TODO replace: createGeometry(graph, trip, stopTimes, hops);
        }
    }

    /**
     * According to the GTFS specification, the first trip depart at the frequency
     * start-time and the trip-pattern time-shift is the time of the first stop
     * arrival. So, we compute the delta between the arrival and departure and
     * subtract that from the frequency start-time to get the initial time-shift value.
     */
    private int timeShiftForFrequencyBasedTrip(TripTimes tripTimes, Frequency frequency) {
        int delta = tripTimes.getDepartureTime(0) - tripTimes.getArrivalTime(0);
        return frequency.getStartTime() - delta;
    }

    private TripPattern findOrCreateTripPattern(StopPattern stopPattern, Route route, Direction direction) {
        for(TripPattern tripPattern : tripPatterns.get(stopPattern)) {
            if(tripPattern.getRoute().equals(route) && tripPattern.getDirection().equals(direction)) {
                return tripPattern;
            }
        }
        FeedScopedId patternId = generateUniqueIdForTripPattern(route, direction.gtfsCode);
        TripPattern tripPattern = new TripPattern(patternId, route, stopPattern);
        tripPatterns.put(stopPattern, tripPattern);
        return tripPattern;
    }

    /**
     * Patterns do not have unique IDs in GTFS, so we make some by concatenating agency id, route
     * id, the direction and an integer. This only works if the Collection of TripPattern includes
     * every TripPattern for the agency.
     */
    private FeedScopedId generateUniqueIdForTripPattern(Route route, int directionId) {
        FeedScopedId routeId = route.getId();
        String direction = directionId != -1 ? String.valueOf(directionId) : "";
        String key = routeId.getId() + ":" + direction;

        // Add 1 to counter and update it
        int counter = tripPatternIdCounters.getOrDefault(key, 0) + 1;
        tripPatternIdCounters.put(key, counter);

        // OBA library uses underscore as separator, we're moving toward colon.
        String id = String.format("%s:%s:%02d", routeId.getId(), direction, counter);

        return new FeedScopedId(routeId.getFeedId(), id);
    }
}
