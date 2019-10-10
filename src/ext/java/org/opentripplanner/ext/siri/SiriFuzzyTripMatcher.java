package org.opentripplanner.ext.siri;

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.VehicleActivityStructure;
import uk.org.siri.siri20.VehicleModesEnumeration;

import java.time.ZonedDateTime;
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
    private static Map<String, Set<Trip>> mappedVehicleRefCache = new HashMap<>();
    private static Map<String, Set<Route>> mappedRoutesCache = new HashMap<>();
    private static Map<String, Set<Trip>> start_stop_tripCache = new HashMap<>();

    private static Map<String, Trip> vehicleJourneyTripCache = new HashMap<>();

    private static Set<String> nonExistingStops = new HashSet<>();

    public SiriFuzzyTripMatcher(GraphIndex index) {
        this.index = index;
        initCache(this.index);
    }

    //For testing only
    protected SiriFuzzyTripMatcher(GraphIndex index, boolean forceCacheRebuild) {
        LOG.error("For testing only");
        this.index = index;

        if (forceCacheRebuild) {
            mappedTripsCache.clear();
        }
        initCache(this.index);
    }

    /**
     * Matches VehicleActivity to a set of possible Trips based on tripId
     */
    public Set<Trip> match(VehicleActivityStructure activity) {
        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = activity.getMonitoredVehicleJourney();
        Set<Trip> trips = new HashSet<>();
        if (monitoredVehicleJourney != null) {

            String datedVehicleRef = null;
            if (monitoredVehicleJourney.getFramedVehicleJourneyRef() != null) {
                datedVehicleRef = monitoredVehicleJourney.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef();
                if (datedVehicleRef != null) {
                    trips = mappedTripsCache.get(datedVehicleRef);
                }
            }
            if (monitoredVehicleJourney.getDestinationRef() != null) {

                String destinationRef = monitoredVehicleJourney.getDestinationRef().getValue();
                ZonedDateTime arrivalTime = monitoredVehicleJourney.getDestinationAimedArrivalTime();

                if (arrivalTime != null) {
                    trips = getMatchingTripsOnStopOrSiblings(destinationRef, arrivalTime);
                }
            }
        }

        return trips;
    }

    public Trip findTripByDatedVehicleJourneyRef(EstimatedVehicleJourney journey) {
        String serviceJourneyId = resolveDatedVehicleJourneyRef(journey);
        if (serviceJourneyId != null) {
            for (String feedId : index.agenciesForFeedId.keySet()) {
                Trip trip = index.tripForId.get(new FeedScopedId(feedId, serviceJourneyId));
                if (trip != null) {
                    return trip;
                }
            }
        }
        return null;
    }

    private String resolveDatedVehicleJourneyRef(EstimatedVehicleJourney journey) {

        if (journey.getFramedVehicleJourneyRef() != null) {
            return journey.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef();
        } else if (journey.getDatedVehicleJourneyRef() != null) {
            return journey.getDatedVehicleJourneyRef().getValue();
        }

        return null;
    }

    /**
     * Matches EstimatedVehicleJourney to a set of possible Trips based on tripId
     */
    public Set<Trip> match(EstimatedVehicleJourney journey) {
        Set<Trip> trips = null;
        if (journey.getVehicleRef() != null &&
                (journey.getVehicleModes() != null && journey.getVehicleModes().contains(VehicleModesEnumeration.RAIL))) {
            trips = getCachedTripsByVehicleRef(journey.getVehicleRef().getValue());
        }

        if (trips == null || trips.isEmpty()) {
            String serviceJourneyId = resolveDatedVehicleJourneyRef(journey);
            if (serviceJourneyId != null) {
                trips = getCachedTripsBySiriId(serviceJourneyId);
            }
        }
        if (trips == null || trips.isEmpty()) {
            List<EstimatedCall> estimatedCalls = journey.getEstimatedCalls().getEstimatedCalls();
            EstimatedCall lastStop = estimatedCalls.get(estimatedCalls.size() - 1);

            String lastStopPoint = lastStop.getStopPointRef().getValue();

            ZonedDateTime arrivalTime = lastStop.getAimedArrivalTime() != null ? lastStop.getAimedArrivalTime() : lastStop.getAimedDepartureTime();

            if (arrivalTime != null) {
                trips = getMatchingTripsOnStopOrSiblings(lastStopPoint, arrivalTime);
            }
        }
        return trips;
    }

    private Set<Trip> getMatchingTripsOnStopOrSiblings(String lastStopPoint, ZonedDateTime arrivalTime) {

        Set<Trip> trips = start_stop_tripCache.get(createStartStopKey(lastStopPoint, arrivalTime.toLocalTime().toSecondOfDay()));
        if (trips == null) {
            //Attempt to fetch trips that started yesterday - i.e. add 24 hours to arrival-time
            int lastStopArrivalTime = arrivalTime.toLocalTime().toSecondOfDay() + (24 * 60 * 60);
            trips = start_stop_tripCache.get(createStartStopKey(lastStopPoint, lastStopArrivalTime));
        }

        if (trips == null || trips.isEmpty()) {
            //SIRI-data may report other platform, but still on the same Parent-stop
            String agencyId = index.agenciesForFeedId.keySet().iterator().next();
            Stop stop = index.stopForId.get(new FeedScopedId(agencyId, lastStopPoint));
            if (stop != null && stop.getParentStation() != null) {
                Collection<Stop> allQuays = index.stopsForParentStation.get(stop.getParentStation());
                for (Stop quay : allQuays) {
                    Set<Trip> tripSet = start_stop_tripCache.get(createStartStopKey(quay.getId().getId(), arrivalTime.toLocalTime().toSecondOfDay()));
                    if (tripSet != null) {
                        if (trips == null) {
                            trips = tripSet;
                        } else {
                            trips.addAll(tripSet);
                        }
                    }
                }
            }
        }
        return trips;
    }

    private Set<Trip> getCachedTripsByVehicleRef(String vehicleRef) {
        if (vehicleRef == null) {return null;}
        return mappedVehicleRefCache.getOrDefault(vehicleRef, new HashSet<>());
    }

    private Set<Trip> getCachedTripsBySiriId(String tripId) {
        if (tripId == null) {return null;}
        return mappedTripsCache.getOrDefault(tripId, new HashSet<>());
    }

    private static void initCache(GraphIndex index) {
        if (mappedTripsCache.isEmpty()) {

            Set<Trip> trips = index.patternForTrip.keySet();
            for (Trip trip : trips) {

                TripPattern tripPattern = index.patternForTrip.get(trip);

                    String currentTripId = getUnpaddedTripId(trip.getId().getId());

                    if (mappedTripsCache.containsKey(currentTripId)) {
                        mappedTripsCache.get(currentTripId).add(trip);
                    } else {
                        Set<Trip> initialSet = new HashSet<>();
                        initialSet.add(trip);
                        mappedTripsCache.put(currentTripId, initialSet);
                    }

                if (tripPattern != null && (tripPattern.mode.equals(TraverseMode.RAIL) /*||
                                                    (trip.getTransportSubmode() != null &&
                                                            trip.getTransportSubmode().equals(TransmodelTransportSubmode.RAIL_REPLACEMENT_BUS))*/)) {
                    // TODO - SIRI: Add support for submode
                    if (trip.getTripShortName() != null) {
                        String tripShortName = trip.getTripShortName();
                        if (mappedVehicleRefCache.containsKey(tripShortName)) {
                            mappedVehicleRefCache.get(tripShortName).add(trip);
                        } else {
                            Set<Trip> initialSet = new HashSet<>();
                            initialSet.add(trip);
                            mappedVehicleRefCache.put(tripShortName, initialSet);
                        }
                    }
                }
                String lastStopId = tripPattern.getStops().get(tripPattern.getStops().size()-1).getId().getId();

                TripTimes tripTimes = tripPattern.scheduledTimetable.getTripTimes(trip);
                if (tripTimes != null) {
                    int arrivalTime = tripTimes.getArrivalTime(tripTimes.getNumStops() - 1);

                    String key = createStartStopKey(lastStopId, arrivalTime);
                    if (start_stop_tripCache.containsKey(key)) {
                        start_stop_tripCache.get(key).add(trip);
                    } else {
                        Set<Trip> initialSet = new HashSet<>();
                        initialSet.add(trip);
                        start_stop_tripCache.put(key, initialSet);
                    }
                }
            }
            Set<Route> routes = index.patternsForRoute.keySet();
            for (Route route : routes) {

                String currentRouteId = getUnpaddedTripId(route.getId().getId());
                if (mappedRoutesCache.containsKey(currentRouteId)) {
                    mappedRoutesCache.get(currentRouteId).add(route);
                } else {
                    Set<Route> initialSet = new HashSet<>();
                    initialSet.add(route);
                    mappedRoutesCache.put(currentRouteId, initialSet);
                }
            }

            LOG.info("Built route-cache [{}].", mappedRoutesCache.size());
            LOG.info("Built vehicleRef-cache [{}].", mappedVehicleRefCache.size());
            LOG.info("Built trips-cache [{}].", mappedTripsCache.size());
            LOG.info("Built start-stop-cache [{}].", start_stop_tripCache.size());
        }

        if (vehicleJourneyTripCache.isEmpty()) {
            index.tripForId.values().forEach(trip -> vehicleJourneyTripCache.put(trip.getId().getId(), trip));
        }
    }

    private static String createStartStopKey(String lastStopId, int lastStopArrivalTime) {
        return lastStopId + ":" + lastStopArrivalTime;
    }

    private static String getUnpaddedTripId(String id) {
        if (id.indexOf("-") > 0) {
            return id.substring(0, id.indexOf("-"));
        } else {
            return id;
        }
    }

    public Set<Route> getRoutesForStop(FeedScopedId siriStopId) {
        Stop stop = index.stopForId.get(siriStopId);
        return index.routesForStop(stop);
    }

    public FeedScopedId getStop(String siriStopId) {

        if (nonExistingStops.contains(siriStopId)) {
            return null;
        }

        //First, assume same agency
        Stop firstStop = index.stopForId.values().stream().findFirst().get();
        FeedScopedId id = new FeedScopedId(firstStop.getId().getAgencyId(), siriStopId);
        if (index.stopForId.containsKey(id)) {
            return id;
        } /*
        TODO: Find StopPlace when Quay is provided
        else if (index.stationForId.containsKey(id)) {
            return id;
        }*/

        //Not same agency - loop through all stops/Stations
        Collection<Stop> stops = index.stopForId.values();
        for (Stop stop : stops) {
            if (stop.getId().getId().equals(siriStopId)) {
                return stop.getId();
            }
        }
        //No match found in quays - check parent-stops (stopplace)
        for (Stop stop : stops) {
            if (siriStopId.equals(stop.getParentStation())) {
                return new FeedScopedId(stop.getId().getAgencyId(), stop.getParentStation());
            }
        }

        nonExistingStops.add(siriStopId);
        return null;
    }

    public Set<Route> getRoutes(String lineRefValue) {
        return mappedRoutesCache.getOrDefault(lineRefValue, new HashSet<>());
    }

    public FeedScopedId getTripId(String vehicleJourney) {
        Trip trip = vehicleJourneyTripCache.get(vehicleJourney);
        if (trip != null) {
            return trip.getId();
        }
        //Fallback to handle extrajourneys
        for (String feedId : index.agenciesForFeedId.keySet()) {
            trip = index.tripForId.get(new FeedScopedId(feedId, vehicleJourney));
            if (trip != null) {
                vehicleJourneyTripCache.put(vehicleJourney, trip);
                return trip.getId();
            }
        }
        return null;
    }

    public List<FeedScopedId> getTripIdForTripShortNameServiceDateAndMode(String tripShortName, ServiceDate serviceDate, TraverseMode traverseMode/*, TransmodelTransportSubmode transportSubmode*/) {

        Set<Trip> cachedTripsBySiriId = getCachedTripsBySiriId(tripShortName);

        if (cachedTripsBySiriId.isEmpty()) {
            cachedTripsBySiriId = getCachedTripsByVehicleRef(tripShortName);
        }

        List<FeedScopedId> matches = new ArrayList<>();
        for (Trip trip : cachedTripsBySiriId) {
            if (GtfsLibrary.getTraverseMode(trip.getRoute()).equals(traverseMode)
                /*|| trip.getTransportSubmode().equals(transportSubmode)*/) {
                Set<ServiceDate> serviceDates = index.graph.getCalendarService().getServiceDatesForServiceId(trip.getServiceId());

                if (serviceDates.contains(serviceDate) &&
                        trip.getTripShortName() != null &&
                        trip.getTripShortName().equals(tripShortName)) {
                    matches.add(trip.getId());
                }
            }
        }


        return matches;
    }

    public int getTripDepartureTime(FeedScopedId tripId) {
        Trip trip = index.tripForId.get(tripId);
        {
            TripPattern tripPattern = index.patternForTrip.get(trip);

            if (tripPattern != null) {
                TripTimes tripTimes = tripPattern.scheduledTimetable.getTripTimes(trip);
                if (tripTimes != null) {
                    return tripTimes.getArrivalTime(0);

                }
            }
        }
        return -1;
    }
    public int getTripArrivalTime(FeedScopedId tripId) {
        Trip trip = index.tripForId.get(tripId);
        {
            TripPattern tripPattern = index.patternForTrip.get(trip);

            if (tripPattern != null) {
                TripTimes tripTimes = tripPattern.scheduledTimetable.getTripTimes(trip);
                if (tripTimes != null) {
                    return tripTimes.getArrivalTime(tripTimes.getNumStops()-1);
                }
            }
        }
        return -1;
    }
}
