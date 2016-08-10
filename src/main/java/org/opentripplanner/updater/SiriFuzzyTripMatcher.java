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
import uk.org.siri.siri20.LocationStructure;
import uk.org.siri.siri20.VehicleActivityStructure;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used for matching TripDescriptors without trip_ids to scheduled GTFS data and to
 * feed back that information into a new TripDescriptor with proper trip_id.
 *
 * The class should only be used if we know that the feed producer is unable to produce trip_ids
 * in the GTFS-RT feed.
 */
public class SiriFuzzyTripMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcher.class);

    private GraphIndex index;

    public SiriFuzzyTripMatcher(GraphIndex index) {
        this.index = index;
    }

    public Trip match(VehicleActivityStructure activity) {

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
                for (Trip trip : trips) {
                    if (trip.getId().getId().equals(tripId)) {
                        return trip;
                    }
                }
                LOG.warn("Trip with id [{}] not found.", tripId);
            }

            if (monitoredVehicleJourney.getVehicleRef() != null) {

                if (monitoredVehicleJourney.isMonitored() == null || !monitoredVehicleJourney.isMonitored()) {
                    //Monitoring error
                    LOG.info("Monitoring-error reported - {}", monitoredVehicleJourney.getMonitoringError());
                    return null;
                }

                ZonedDateTime originAimedDepartureTime = monitoredVehicleJourney.getOriginAimedDepartureTime();

                int time = -1;
                if (originAimedDepartureTime != null) {

                    int ex = originAimedDepartureTime.getHour();
                    int minutes = originAimedDepartureTime  .getMinute();
                    int seconds = originAimedDepartureTime.getSecond();
                    time = seconds + 60 * (minutes + 60 * ex);
                }
                String routeId = (monitoredVehicleJourney.getLineRef() != null ? monitoredVehicleJourney.getLineRef().getValue():null);

                List<Route> routes = getRoute(routeId);

                ServiceDate date = new ServiceDate();
                if (monitoredVehicleJourney.getOriginAimedDepartureTime() != null) {
                    ZonedDateTime departureTime = monitoredVehicleJourney.getOriginAimedDepartureTime();
                    date = new ServiceDate(departureTime.getYear(), departureTime.getMonthValue(), departureTime.getDayOfMonth());
                }


                for (Route route : routes) {
                    int direction = getDirection(route, monitoredVehicleJourney);
                    Trip matchedTrip = getTrip(route, direction, time, date);
                    if (matchedTrip != null) {
                        return matchedTrip;
                    }
                }
            }

        } else if (monitoredVehicleJourney != null && monitoredVehicleJourney.getVehicleLocation() != null) {
            //Is VehicleLocation relevant???
            LocationStructure vehicleLocation = monitoredVehicleJourney.getVehicleLocation();
            LOG.info("Vehicle {} is located at {}, {}.", monitoredVehicleJourney.getVehicleRef().getValue(), vehicleLocation.getLongitude(), vehicleLocation.getLatitude());
        }


            return null;
    }

    private List<Route> getRoute(String routeId) {
        Set<AgencyAndId> agencyAndIds = index.routeForId.keySet();
        List<Route> routes = new ArrayList<>();
        for (AgencyAndId agencyAndId : agencyAndIds) {
            if (agencyAndId.getId().equals(routeId)) {
                routes.add(index.routeForId.get(agencyAndId));
            }
        }
        return routes;
    }


    /*
     * Finds the direction of the trip/route/journey
     * direction == 1 : start -> end
     * direction == 0 : end -> start
     */
    private int getDirection(Route route, VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney) {
        JourneyPlaceRefStructure originRefStructure = monitoredVehicleJourney.getOriginRef();
        DestinationRef destinationRefStructure = monitoredVehicleJourney.getDestinationRef();

        if (originRefStructure == null) {
            return 0;
        }
        if (destinationRefStructure == null) {
            return 0;
        }

        if (monitoredVehicleJourney.getDirectionRef() != null) {
            String direction = monitoredVehicleJourney.getDirectionRef().getValue();

            if ("back".equals(direction)) {
                return 0;
            }
            if ("go".equals(direction)) {
                return 1;
            }
        }

        String destinationRef = destinationRefStructure.getValue();
        String originRef = originRefStructure.getValue();


        for (TripPattern pattern : index.patternsForRoute.get(route)) {
            List<Stop> stops = pattern.getStops();

            if (stops != null && stops.size() > 2) {

                String firstId = stops.get(0).getId().getId();
                String lastId = stops.get(stops.size()-1).getId().getId();

                if (firstId.equals(originRef) && lastId.equals(destinationRef)) {
                    return 1;
                }
                if (lastId.equals(originRef) && firstId.equals(destinationRef)) {
                    return 0;
                }
            }
        }
        return -1;
    }

    public Trip getTrip (Route route, int direction,  int startTime, ServiceDate date) {
        BitSet services = index.servicesRunning(date);
        for (TripPattern pattern : index.patternsForRoute.get(route)) {
            if (pattern.directionId != direction) {
                List<Stop> stops = pattern.getStops();
                continue;
            }
            for (TripTimes times : pattern.scheduledTimetable.tripTimes) {
                if (times.getScheduledDepartureTime(0) == startTime &&
                        services.get(times.serviceCode)) {
                    return times.trip;
                }
            }
        }
        return null;
    }
}
