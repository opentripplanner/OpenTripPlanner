package org.opentripplanner.transit;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Stop;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.opentripplanner.streets.StreetLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class TransitLayer implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayer.class);

    // Do we really need to store this? It is a key into the GTFS MapDB.
    public List<String> stopIdForIndex = new ArrayList<>();

    public static final int TYPICAL_NUMBER_OF_STOPS_PER_TRIP = 30;

    public List<TripPattern> tripPatterns = new ArrayList<>();

    // Maybe we need a StopStore that has (streetVertexForStop, transfers, flags, etc.)
    public TIntList streetVertexForStop = new TIntArrayList();

    // Inverse map of streetVertexForStop, and reconstructed from that list.
    public transient TIntIntMap stopForStreetVertex;

    // For each stop, a packed list of transfers to other stops
    public List<TIntList> transfersForStop;

    public List<TIntList> patternsForStop;

    // Seems kind of hackish to pass the street layer in.
    // Maybe there should not even be separate street and transit layer classes.
    // Should we have separate stop objects in the transit layer and stop vertices in the street layer?
    public void loadFromGtfs (GTFSFeed gtfs, StreetLayer streetLayer) {

        // Load stops.
        // ID is the GTFS string ID, stopIndex is the zero-based index, stopVertexIndex is the index in the street layer.
        TObjectIntMap<String> indexForStopId = new TObjectIntHashMap<>();
        for (Stop stop : gtfs.stops.values()) {
            int stopIndex = stopIdForIndex.size();
            indexForStopId.put(stop.stop_id, stopIndex);
            stopIdForIndex.add(stop.stop_id);
            if (streetLayer != null) {
                int streetVertexIndex = streetLayer.linkTransitStop(stop.stop_lat, stop.stop_lon, 300);
                streetVertexForStop.add(streetVertexIndex);
            }
        }

        // Group trips by stop pattern (including pickup/dropoff type) and fill stop times into patterns.
        LOG.info("Grouping trips by stop pattern and creating trip schedules.");
        Map<TripPatternKey, TripPattern> tripPatternForStopSequence = new HashMap<>();
        int nTripsAdded = 0;
        for (String tripId : gtfs.trips.keySet()) {
            Trip trip = gtfs.trips.get(tripId);
            // Construct the stop pattern and schedule for this trip
            // Should we really be resolving to an object reference for Route?
            // That gets in the way of GFTS persistence.
            TripPatternKey tripPatternKey = new TripPatternKey(trip.route.route_id);
            TIntList arrivals = new TIntArrayList(TYPICAL_NUMBER_OF_STOPS_PER_TRIP);
            TIntList departures = new TIntArrayList(TYPICAL_NUMBER_OF_STOPS_PER_TRIP);
            for (StopTime st : gtfs.getOrderedStopTimesForTrip(tripId)) {
                int stopIndex = indexForStopId.get(st.stop_id);
                tripPatternKey.addStopTime(st, indexForStopId);
                arrivals.add(st.arrival_time);
                departures.add(st.departure_time);
            }
            TripPattern tripPattern = tripPatternForStopSequence.get(tripPatternKey);
            if (tripPattern == null) {
                tripPattern = new TripPattern(tripPatternKey);
                tripPatternForStopSequence.put(tripPatternKey, tripPattern);
                tripPatterns.add(tripPattern);
            }
            tripPattern.setOrVerifyDirection(trip.direction_id);
            tripPattern.addTrip(new TripSchedule(trip, arrivals.toArray(), departures.toArray()));
            nTripsAdded += 1;
        }
        LOG.info("Done creating {} trips on {} patterns.", nTripsAdded, tripPatternForStopSequence.size());

        // Will be useful in naming patterns.
//        LOG.info("Finding topology of each route/direction...");
//        Multimap<T2<String, Integer>, TripPattern> patternsForRouteDirection = HashMultimap.create();
//        tripPatterns.forEach(tp -> patternsForRouteDirection.put(new T2(tp.routeId, tp.directionId), tp));
//        for (T2<String, Integer> routeAndDirection : patternsForRouteDirection.keySet()) {
//            RouteTopology topology = new RouteTopology(routeAndDirection.first, routeAndDirection.second, patternsForRouteDirection.get(routeAndDirection));
//        }

    }

    /** (Re-)build transient indexes of this TripPattern, connecting stops to patterns etc. */
    public void rebuildTransientIndexes () {

        // 1. Which patterns pass through each stop?
        // We could store references to patterns rather than indexes.
        int nStops = stopIdForIndex.size();
        patternsForStop = new ArrayList<>(nStops);
        for (int i = 0; i < nStops; i++) {
            patternsForStop.add(new TIntArrayList());
        }
        int p = 0;
        for (TripPattern pattern : tripPatterns) {
            for (int stopIndex : pattern.stops) {
                if (!patternsForStop.get(stopIndex).contains(p)) {
                    patternsForStop.get(stopIndex).add(p);
                }
            }
            p++;
        }

        // 2. What street vertex represents each transit stop? Invert the serialized map.
        stopForStreetVertex = new TIntIntHashMap();
        for (int s = 0; s < streetVertexForStop.size(); s++) {
            stopForStreetVertex.put(streetVertexForStop.get(s), s);
        }
    }

    public static TransitLayer fromGtfs (String file, StreetLayer streetLayer) {
        GTFSFeed gtfs = GTFSFeed.fromFile(file);
        TransitLayer transitLayer = new TransitLayer();
        transitLayer.loadFromGtfs(gtfs, streetLayer);
        return transitLayer;
    }

    public int getStopCount () {
        return stopIdForIndex.size();
    }

}
