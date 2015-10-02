package org.opentripplanner.transit;

import org.opentripplanner.profile.RaptorWorkerTimetable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/**
 * This is like a transmodel JourneyPattern.
 * All the trips on the same Route that have the same sequence of stops, with the same pickup/dropoff options.
 */
public class TripPattern implements Serializable {

    private static Logger LOG = LoggerFactory.getLogger(TripPattern.class);

    String routeId;
    int directionId = Integer.MIN_VALUE;
    int[] stops;
    // Could be compacted into 2 bits each or a bunch of flags, but is it even worth it?
    PickDropType[] pickups;
    PickDropType[] dropoffs;
    BitSet wheelchairAccessible; // One bit per stop
    List<TripSchedule> tripSchedules = new ArrayList<>();

    // This set includes the numeric codes for all services on which at least one trip in this pattern is active.
    BitSet servicesActive = new BitSet();

    public TripPattern (TripPatternKey tripPatternKey) {
        int nStops = tripPatternKey.stops.size();
        stops = new int[nStops];
        pickups = new PickDropType[nStops];
        dropoffs = new PickDropType[nStops];
        wheelchairAccessible = new BitSet(nStops);
        for (int s = 0; s < nStops; s++) {
            stops[s] = tripPatternKey.stops.get(s);
            pickups[s] = PickDropType.forGtfsCode(tripPatternKey.pickupTypes.get(s));
            dropoffs[s] = PickDropType.forGtfsCode(tripPatternKey.dropoffTypes.get(s));
        }
        routeId = tripPatternKey.routeId;
    }

    public void addTrip (TripSchedule tripSchedule) {
        tripSchedules.add(tripSchedule);
        servicesActive.set(tripSchedule.serviceCode);
    }

    public void setOrVerifyDirection (int directionId) {
        if (this.directionId != directionId) {
            if (this.directionId == Integer.MIN_VALUE) {
                this.directionId = directionId;
                LOG.debug("Pattern has route_id {} and direction_id {}", routeId, directionId);
            } else {
                LOG.warn("Trips with different direction IDs are in the same pattern.");
            }
        }
    }

    // Simply write "graph builder annotations" to a log file alongside the graphs.
    // function in gtfs-lib getOrderedStopTimes(string tripId)
    // Test GTFS loading on NL large data set.

    /**
     * Linear search.
     * @return null if no departure is possible.
     */
    TripSchedule findNextDeparture (int time, int stopOffset) {
        TripSchedule bestSchedule = null;
        int bestTime = Integer.MAX_VALUE;
        for (TripSchedule schedule : tripSchedules) {
            boolean active = servicesActive.get(schedule.serviceCode);
            // LOG.info("Trip with service {} active: {}.", schedule.serviceCode, active);
            if (servicesActive.get(schedule.serviceCode)) {
                int departureTime = schedule.departures[stopOffset];
                if (departureTime > time && departureTime < bestTime) {
                    bestTime = departureTime;
                    bestSchedule = schedule;
                }
            }
        }
        return bestSchedule;
    }

    /**
     * Convert a new-style TransitNetwork TripPattern to a RaptorWorkerTimetable as a stopgap measure, to allow
     * making RaptorWorkerData from the new TransitNetwork.
     */
    public RaptorWorkerTimetable toRaptorWorkerTimetable (BitSet servicesActive) {
        List<TripSchedule> activeSchedules = new ArrayList<>();
        for (TripSchedule schedule: tripSchedules) {
            if (servicesActive.get(schedule.serviceCode)) {
                activeSchedules.add(schedule);
            }
        }
        // The Raptor worker expects trips to be sorted by departure time.
        Collections.sort(activeSchedules);
        RaptorWorkerTimetable timetable = new RaptorWorkerTimetable(activeSchedules.size(), stops.length);
        int trip = 0;
        for (TripSchedule schedule : activeSchedules) {
            int nStops = schedule.arrivals.length;
            // The Raptor worker has arrivals and departures packed into a single array.
            // This is probably not very beneficial.
            int[] packed = new int[nStops * 2];
            for (int s = 0, ps = 0; s < nStops; s++) {
                packed[ps++] = schedule.arrivals[s];
                packed[ps++] = schedule.departures[s];
            }
            timetable.timesPerTrip[trip++] = packed;
        }

        timetable.stopIndices = this.stops;

// TODO: more fields in timetable
//        /** parent raptorworkerdata of this timetable */
//        public RaptorWorkerData raptorData;
//        /** Mode of this pattern, see constants in com.conveyal.gtfs.model.Route */
//        public int mode;
//        /** Index of this pattern in RaptorData */
//        public int dataIndex;

        return timetable;
    }

}
