package org.opentripplanner.updater;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.graph.GraphIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.*;

import java.util.*;

/**
 * This class is used for matching TripDescriptors without trip_ids to scheduled GTFS data and to
 * feed back that information into a new TripDescriptor with proper trip_id.
 *
 */
public class SiriFuzzyTripMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcher.class);

    private GraphIndex index;

    private static Map<String, Set<Trip>> mappedTripsCache = new HashMap<>();

    public SiriFuzzyTripMatcher(GraphIndex index) {
        this.index = index;
        initCache(this.index);
    }

    /**
     * Matches VehicleActivity to a set of possible Trips based on tripId
     */
    public Set<Trip> match(VehicleActivityStructure activity) {
        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = activity.getMonitoredVehicleJourney();

        if (monitoredVehicleJourney != null) {

            String tripId = null;
            if (monitoredVehicleJourney.getCourseOfJourneyRef() != null) {
                tripId = monitoredVehicleJourney.getCourseOfJourneyRef().getValue();
            }
            //TripId is provided in VM-delivery
            return getCachedTripsBySiriId(tripId);
        }

        return null;
    }

    /**
     * Matches EstimatedVehicleJourney to a set of possible Trips based on tripId
     */
    public Set<Trip> match(EstimatedVehicleJourney journey) {

        VehicleJourneyRef vehicleJourney = journey.getVehicleJourneyRef();

        if (vehicleJourney != null) {
            return getCachedTripsBySiriId(vehicleJourney.getValue());
        }

        return null;
    }

    private Set<Trip> getCachedTripsBySiriId(String tripId) {
        if (tripId == null) {return null;}
        return mappedTripsCache.get(tripId);
    }

    private static void initCache(GraphIndex index) {
        if (mappedTripsCache.isEmpty()) {
            Map<String, Set<Trip>> updatedCache = new HashMap<>();

            Set<Trip> trips = index.patternForTrip.keySet();
            for (Trip trip : trips) {

                String currentTripId = getUnpaddedTripId(trip);

                if (updatedCache.containsKey(currentTripId)) {
                    updatedCache.get(currentTripId).add(trip);
                } else {
                    Set<Trip> initialSet = new HashSet<>();
                    initialSet.add(trip);
                    updatedCache.put(currentTripId, initialSet);
                }
            }

            mappedTripsCache = updatedCache;
            LOG.trace("Built trips-cache [{}].", mappedTripsCache.size());
        }
    }

    private static String getUnpaddedTripId(Trip trip) {
        String id = trip.getId().getId();
        if (id.indexOf("-") > 0) {
            return id.substring(0, id.indexOf("-"));
        } else {
            return id;
        }
    }
}
