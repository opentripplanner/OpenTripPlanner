package org.opentripplanner.updater;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.DestinationRef;
import uk.org.siri.siri20.JourneyPlaceRefStructure;
import uk.org.siri.siri20.VehicleActivityStructure;

import java.util.*;

/**
 * This class is used for matching TripDescriptors without trip_ids to scheduled GTFS data and to
 * feed back that information into a new TripDescriptor with proper trip_id.
 *
 */
public class SiriFuzzyTripMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcher.class);

    private GraphIndex index;

    public SiriFuzzyTripMatcher(GraphIndex index) {
        this.index = index;
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

            if (tripId != null) {
                //TripId is provided in VM-delivery
                //Exact tripId is provided
                Set<Trip> trips = index.patternForTrip.keySet();
                Set<Trip> matches = new HashSet<>();
                for (Trip trip : trips) {
                    if (trip.getId().getId().startsWith(tripId + "-")) {
                        matches.add(trip);
                    }
                }
                LOG.trace("Found trip-matches [{}].", matches.size());
                return matches;
            }
        }

        return null;
    }
}
