package org.opentripplanner.ext.siri;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A synchronized cache of trip patterns that are added to the graph due to GTFS-realtime messages.
 */
public class SiriTripPatternCache {

    private static final Logger log = LoggerFactory.getLogger(SiriTripPatternCache.class);

    private int counter = 0;

    private final Map<StopPatternServiceDateKey, TripPattern> cache = new HashMap<>();

    private final ListMultimap<Stop, TripPattern> patternsForStop = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());

    private final Map<TripServiceDateKey, TripPattern> updatedTripPatternsForTripCache = new HashMap<>();

    /**
     * Get cached trip pattern or create one if it doesn't exist yet. If a trip pattern is created, vertices
     * and edges for this trip pattern are also created in the graph.
     * 
     * @param stopPattern stop pattern to retrieve/create trip pattern
     * @param trip Trip containing route of new trip pattern in case a new trip pattern will be created
     * @param graph graph to add vertices and edges in case a new trip pattern will be created
     * @param serviceDate
     * @return cached or newly created trip pattern
     */
    public synchronized TripPattern getOrCreateTripPattern(
            @NotNull final StopPattern stopPattern,
            @NotNull final Trip trip,
            @NotNull final Graph graph,
            @NotNull ServiceDate serviceDate) {
        // Check cache for trip pattern
        StopPatternServiceDateKey key = new StopPatternServiceDateKey(stopPattern, serviceDate);
        TripPattern tripPattern = cache.get(key);
        
        // Create TripPattern if it doesn't exist yet
        if (tripPattern == null) {
            tripPattern = new TripPattern(trip.getRoute(), stopPattern);

            // Generate unique code for trip pattern
            //TODO - SIRI: Is this a good way to generate new trippPattern.id?
            tripPattern.setId(new FeedScopedId(trip.getId().getFeedId(), generateUniqueTripPatternCode(tripPattern)));
            
            // Create an empty bitset for service codes (because the new pattern does not contain any trips)
            tripPattern.setServiceCodes(graph.getServiceCodes());
            
            // Finish scheduled time table
            tripPattern.scheduledTimetable.finish();
            
            // Create vertices and edges for new TripPattern
            // TODO: purge these vertices and edges once in a while?
//            tripPattern.makePatternVerticesAndEdges(graph, graph.index.stopVertexForStop);
            
            // TODO - SIRI: Add pattern to graph index?

            TripPattern originalTripPattern = graph.index.getPatternForTrip().get(trip);

            // Copy information from the TripPattern this is replacing
            if (originalTripPattern != null) {
                tripPattern.setId(originalTripPattern.getId());
                tripPattern.setHopGeometriesFromPattern(originalTripPattern);
            }
            
            // Add pattern to cache
            cache.put(key, tripPattern);

        }

        /**
         *
         * When the StopPattern is first modified (e.g. change of platform), then updated (or vice versa), the stopPattern is altered, and
         * the StopPattern-object for the different states will not be equal.
         *
         * This causes both tripPatterns to be added to all unchanged stops along the route, which again causes duplicate results
         * in departureRow-searches (one departure for "updated", one for "modified").
         *
         * Full example:
         *      Planned stops: Stop 1 - Platform 1, Stop 2 - Platform 1
         *
         *      StopPattern #rt1: "updated" stopPattern cached in 'patternsForStop':
         *          - Stop 1, Platform 1
         *          	- StopPattern #rt1
         *          - Stop 2, Platform 1
         *          	- StopPattern #rt1
         *
         *      "modified" stopPattern: Stop 1 - Platform 1, Stop 2 - Platform 2
         *
         *      StopPattern #rt2: "modified" stopPattern cached in 'patternsForStop' will then be:
         *          - Stop 1, Platform 1
         *          	- StopPattern #rt1, StopPattern #rt2
         *          - Stop 2, Platform 1
         *          	- StopPattern #rt1
         *          - Stop 2, Platform 2
         *          	- StopPattern #rt2
         *
         *
         * Therefore, we must cleanup the duplicates by deleting the previously added (and thus outdated)
         * tripPattern for all affected stops. In example above, "StopPattern #rt1" should be removed from all stops
         *
         */
        TripServiceDateKey tripServiceDateKey = new TripServiceDateKey(trip, serviceDate);
        if (updatedTripPatternsForTripCache.containsKey(tripServiceDateKey)) {
            /**
             * Remove previously added TripPatterns for the trip currently being updated - if the stopPattern does not match
             */
            TripPattern cachedTripPattern = updatedTripPatternsForTripCache.get(tripServiceDateKey);
            if (cachedTripPattern != null && !tripPattern.stopPattern.equals(cachedTripPattern.stopPattern)) {
                int sizeBefore = patternsForStop.values().size();
                long t1 = System.currentTimeMillis();
                patternsForStop.values().removeAll(Arrays.asList(cachedTripPattern));
                int sizeAfter = patternsForStop.values().size();

                log.info("Removed outdated TripPattern for {} stops in {} ms - tripId: {}", (sizeBefore-sizeAfter),  (System.currentTimeMillis()-t1), trip.getId());
                /*
                  TODO: Also remove previously updated - now outdated - TripPattern from cache ?
                  cache.remove(new StopPatternServiceDateKey(cachedTripPattern.stopPattern, serviceDate));
                */
            }
        }

        // To make these trip patterns visible for departureRow searches.
        for (Stop stop: tripPattern.getStops()) {
            if (!patternsForStop.containsEntry(stop, tripPattern)) {
                patternsForStop.put(stop, tripPattern);
            }
        }

        /**
         * Cache the last added tripPattern that has been used to update a specific trip
         */
        updatedTripPatternsForTripCache.put(tripServiceDateKey, tripPattern);

        return tripPattern;
    }

    /**
     * Generate unique trip pattern code for real-time added trip pattern. This function roughly
     * follows the format of {@link TripPattern#generateUniqueIds(java.util.Collection)}.
     * 
     * @param tripPattern trip pattern to generate code for
     * @return unique trip pattern code
     */
    private String generateUniqueTripPatternCode(TripPattern tripPattern) {
        FeedScopedId routeId = tripPattern.route.getId();
        String direction = tripPattern.directionId != -1 ? String.valueOf(tripPattern.directionId) : "";
        if (counter == Integer.MAX_VALUE) {
            counter = 0;
        } else {
            counter++;
        }
        // OBA library uses underscore as separator, we're moving toward colon.
        String code = String.format("%s:%s:rt#%d", routeId.getId(), direction, counter);
        return code;
    }

    /**
     * Returns any new TripPatterns added by real time information for a given stop.
     *
     * @param stop the stop
     * @return list of TripPatterns created by real time sources for the stop.
     */
    public List<TripPattern> getAddedTripPatternsForStop(Stop stop) {
        return patternsForStop.get(stop);
    }


}
class StopPatternServiceDateKey {
    StopPattern stopPattern;
    ServiceDate serviceDate;

    public StopPatternServiceDateKey(StopPattern stopPattern, ServiceDate serviceDate) {
        this.stopPattern = stopPattern;
        this.serviceDate = serviceDate;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (!(thatObject instanceof StopPatternServiceDateKey)) return false;
        StopPatternServiceDateKey that = (StopPatternServiceDateKey) thatObject;
        return (this.stopPattern.equals(that.stopPattern) & this.serviceDate.equals(that.serviceDate));
    }

    @Override
    public int hashCode() {
        return stopPattern.hashCode()+serviceDate.hashCode();
    }
}
class TripServiceDateKey {
    Trip trip;
    ServiceDate serviceDate;

    public TripServiceDateKey(Trip trip, ServiceDate serviceDate) {
        this.trip = trip;
        this.serviceDate = serviceDate;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (!(thatObject instanceof TripServiceDateKey)) return false;
        TripServiceDateKey that = (TripServiceDateKey) thatObject;
        return (this.trip.equals(that.trip) & this.serviceDate.equals(that.serviceDate));
    }

    @Override
    public int hashCode() {
        return trip.hashCode()+serviceDate.hashCode();
    }
}