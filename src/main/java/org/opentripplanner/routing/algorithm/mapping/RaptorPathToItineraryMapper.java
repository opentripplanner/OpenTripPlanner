package org.opentripplanner.routing.algorithm.mapping;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.util.PolylineEncoder;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * This maps the paths found by the Raptor search algorithm to the itinerary structure currently used by OTP. The paths,
 * access/egress transfers and transit layer only contains the minimal information needed for routing. Additional
 * information has to be fetched from the graph index to create complete itineraries that can be shown in a trip
 * planner.
 */
public class RaptorPathToItineraryMapper {

    private final TransitLayer transitLayer;

    private final RoutingRequest request;

    private final ZonedDateTime startOfTime;


    /**
     * Constructs an itinerary mapper for a request and a set of results
     *
     * @param transitLayer the currently active transit layer (may have real-time data applied)
     * @param startOfTime the point in time all times in seconds are counted from
     * @param request the current routing request
     */
    public RaptorPathToItineraryMapper(
            TransitLayer transitLayer,
            ZonedDateTime startOfTime,
            RoutingRequest request) {

        this.transitLayer = transitLayer;
        this.startOfTime = startOfTime;
        this.request = request;
    }

    public Itinerary createItinerary(Path<TripSchedule> path) {
        List<Leg> legs = new ArrayList<>();

        // Map access leg
        mapAccessLeg(legs, path.accessLeg());

        // TODO: Add back this code when PathLeg interface contains object references

        PathLeg<TripSchedule> pathLeg = path.accessLeg().nextLeg();

        boolean firstLeg = true;

        while (!pathLeg.isEgressLeg()) {
            // Map transit leg
            if (pathLeg.isTransitLeg()) {
                Leg transitLeg = mapTransitLeg(request, pathLeg.asTransitLeg(), firstLeg);
                firstLeg = false;
                legs.add(transitLeg);
            }

            // Map transfer leg
            if (pathLeg.isTransferLeg()) {
                mapTransferLeg(legs, pathLeg.asTransferLeg());
            }

            pathLeg = pathLeg.nextLeg();
        }

        // Map egress leg
        EgressPathLeg<TripSchedule> egressPathLeg = pathLeg.asEgressLeg();
        mapEgressLeg(legs, egressPathLeg);
        propagateStopPlaceNamesToWalkingLegs(legs);

        Itinerary itinerary = new Itinerary(legs);

        // Map general itinerary fields
        itinerary.generalizedCost = path.cost();
        itinerary.nonTransitLimitExceeded = itinerary.nonTransitDistanceMeters > request.maxWalkDistance;

        return itinerary;
    }

    private void mapAccessLeg(
            List<Leg> legs,
            AccessPathLeg<TripSchedule> accessPathLeg
    ) {
        AccessEgress accessPath = (AccessEgress) accessPathLeg.access();

        if (accessPath.durationInSeconds() == 0) { return; }

        GraphPath graphPath = new GraphPath(accessPath.getLastState(), false);

        Itinerary subItinerary = GraphPathToItineraryMapper
            .generateItinerary(graphPath, false, true, request.locale);

        subItinerary.timeShiftToStartAt(createCalendar(accessPathLeg.fromTime()));

        legs.addAll(subItinerary.legs);
    }

    private Leg mapTransitLeg(
            RoutingRequest request,
            TransitPathLeg<TripSchedule> pathLeg,
            boolean firstLeg
    ) {
        Leg leg = new Leg();

        Stop boardStop = transitLayer.getStopByIndex(pathLeg.fromStop());
        Stop alightStop = transitLayer.getStopByIndex(pathLeg.toStop());
        TripSchedule tripSchedule = pathLeg.trip();
        TripTimes tripTimes = tripSchedule.getOriginalTripTimes();
        TripPattern tripPattern = tripSchedule.getOriginalTripPattern();
        Trip trip = tripTimes.trip;
        Route route = tripPattern.route;
        int numStopsInPattern = tripTimes.getNumStops();

        // Find stop positions in pattern where this leg boards and alights.
        // We cannot assume every stop appears only once in a pattern, so we match times instead of stops.
        int boardStopIndexInPattern = -1;
        int alightStopIndexInPattern = -1;
        for (int s = 0; s < numStopsInPattern; s++) {
            if (pathLeg.fromTime() == tripSchedule.departure(s)) {
                boardStopIndexInPattern = s;
            }
        }
        for (int s = 0; s < numStopsInPattern; s++) {
            if (pathLeg.toTime() == tripSchedule.arrival(s)) {
                alightStopIndexInPattern = s;
            }
        }

        // Include real-time information in the Leg.
        if (!tripTimes.isScheduled()) {
            leg.realTime = true;
            leg.departureDelay = tripTimes.getDepartureDelay(boardStopIndexInPattern);
            leg.arrivalDelay = tripTimes.getArrivalDelay(alightStopIndexInPattern);
        }

        leg.serviceDate = new ServiceDate(request.getDateTime()).asCompactString(); // TODO: This has to be changed for multi-day searches
        leg.intermediateStops = new ArrayList<>();
        leg.startTime = createCalendar(pathLeg.fromTime());
        leg.endTime = createCalendar(pathLeg.toTime());
        leg.mode = TraverseMode.fromTransitMode(tripPattern.getMode());
        leg.tripId = trip.getId();
        leg.from = mapStopToPlace(boardStop, boardStopIndexInPattern);
        leg.to = mapStopToPlace(alightStop, alightStopIndexInPattern);
        List<Coordinate> transitLegCoordinates = extractTransitLegCoordinates(pathLeg);
        leg.legGeometry = PolylineEncoder.createEncodings(transitLegCoordinates);
        leg.distanceMeters = getDistanceFromCoordinates(transitLegCoordinates);

        if (request.showIntermediateStops) {
            leg.intermediateStops = extractIntermediateStops(pathLeg);
        }

        leg.route = route.getLongName();
        leg.routeId = route.getId();
        leg.agencyName = route.getAgency().getName();
        leg.routeColor = route.getColor();
        leg.tripShortName = trip.getTripShortName();
        leg.agencyId = route.getAgency().getId();
        leg.routeShortName = route.getShortName();
        leg.routeLongName = route.getLongName();
        leg.headsign = tripTimes.getHeadsign(boardStopIndexInPattern);
        leg.walkSteps = new ArrayList<>();

        // TODO OTP2 - alightRule and boardRule needs mapping
        //    Under Raptor, for transit trips, ItineraryMapper converts Path<TripSchedule> directly to Itinerary
        //    (the old API response element, within TripPlan). Non-transit trips still use GraphPathToTripPlanConverter
        //    to turn A* results (sequences of States and Edges called GraphPaths) into TripPlans which also contain
        //    Itineraries.
        //    So transit results do not go through GraphPathToTripPlanConverter. It contains logic to find board/alight
        //    rules from StopPatterns within TripPatterns, and attach them as machine-readable codes on Legs, but only
        //    where you are really boarding or alighting (considering interlining / in-seat transfers).
        //    That needs to be re-implemented for the Raptor transit case.
        //    - See e2118e0a -> GraphPathToTripPlanConverter#fixupLegs(List<Leg>, State[][]))
        // leg.alightRule = <Assign here>;
        // leg.boardRule =  <Assign here>;

        AlertToLegMapper.addAlertPatchesToLeg(
            request.getRoutingContext().graph,
            leg,
            firstLeg,
            request.locale
        );

        return leg;
    }

    private void mapTransferLeg(List<Leg> legs, TransferPathLeg<TripSchedule> pathLeg) {
        Stop transferFromStop = transitLayer.getStopByIndex(pathLeg.fromStop());
        Stop transferToStop = transitLayer.getStopByIndex(pathLeg.toStop());
        Transfer transfer = transitLayer.getTransferByStopIndex().get(pathLeg.fromStop()).stream().filter(t -> t.getToStop() == pathLeg.toStop()).findFirst().get();

        Place from = mapStopToPlace(transferFromStop, null);
        Place to = mapStopToPlace(transferToStop, null);
        mapNonTransitLeg(legs, pathLeg, transfer, from, to, false);
    }

    private void mapEgressLeg(
            List<Leg> legs,
            EgressPathLeg<TripSchedule> egressPathLeg
    ) {
        AccessEgress egressPath = (AccessEgress) egressPathLeg.egress();

        if (egressPath.durationInSeconds() == 0) { return; }

        GraphPath graphPath = new GraphPath(egressPath.getLastState(), false);

        Itinerary subItinerary = GraphPathToItineraryMapper
            .generateItinerary(graphPath, false, true, request.locale);

        subItinerary.timeShiftToStartAt(createCalendar(egressPathLeg.fromTime()));

        legs.addAll(subItinerary.legs);
    }

    private void mapNonTransitLeg(List<Leg> legs, PathLeg<TripSchedule> pathLeg, Transfer transfer, Place from, Place to, boolean onlyIfNonZeroDistance) {
        List<Edge> edges = transfer.getEdges();
        if (edges.isEmpty()) {
            Leg leg = new Leg();
            leg.from = from;
            leg.to = to;
            leg.startTime = createCalendar(pathLeg.fromTime());
            leg.endTime = createCalendar(pathLeg.toTime());
            leg.mode = TraverseMode.WALK;
            leg.legGeometry = PolylineEncoder.createEncodings(transfer.getCoordinates());
            leg.distanceMeters = (double) transfer.getDistanceMeters();
            leg.walkSteps = Collections.emptyList();

            if (!onlyIfNonZeroDistance || leg.distanceMeters > 0) {
                legs.add(leg);
            }
        } else {
            RoutingRequest traverseRequest = request.clone();
            traverseRequest.arriveBy = false;
            StateEditor se = new StateEditor(traverseRequest, edges.get(0).getFromVertex());
            se.setTimeSeconds(startOfTime.plusSeconds(pathLeg.fromTime()).toEpochSecond());
            //se.setNonTransitOptionsFromState(states[0]);
            State s = se.makeState();
            ArrayList<State> transferStates = new ArrayList<>();
            transferStates.add(s);
            for (Edge e : edges) {
                s = e.traverse(s);
                transferStates.add(s);
            }

            try {
                State[] states = transferStates.toArray(new State[0]);
                GraphPath graphPath = new GraphPath(states[states.length - 1], false);

                Itinerary subItinerary = GraphPathToItineraryMapper
                        .generateItinerary(graphPath, false, true, request.locale);

                // TODO OTP2 We use the duration initially calculated for use during routing
                //      because they do not always match up and we risk getting negative wait times
                //      (#2955)
                if (subItinerary.legs.size() != 1) {
                    throw new IllegalArgumentException("Sub itineraries should only contain one leg.");
                }
                subItinerary.legs.get(0).startTime = createCalendar(pathLeg.fromTime());
                subItinerary.legs.get(0).endTime = createCalendar(pathLeg.toTime());

                if (!onlyIfNonZeroDistance || subItinerary.nonTransitDistanceMeters > 0) {
                    legs.addAll(subItinerary.legs);
                }
            } catch (TrivialPathException e) {
                // Ignore, no legs need be copied
            }
        }
    }

    /**
     * Fix up a {@link Place} using the information available at the leg boundaries. This method
     * will ensure that stop names propagate correctly to the non-transit legs that connect to
     * transit legs.
     */
    public static void propagateStopPlaceNamesToWalkingLegs(List<Leg> legs) {
        for (int i = 0; i < legs.size()-1; i++) {
            Leg currLeg = legs.get(i);
            Leg nextLeg = legs.get(i + 1);

            if (currLeg.isTransitLeg() && !nextLeg.isTransitLeg()) {
                nextLeg.from = currLeg.to;
            }

            if (!currLeg.isTransitLeg() && nextLeg.isTransitLeg()) {
                currLeg.to = nextLeg.from;
            }
        }
    }

    private Place mapOriginTargetToPlace(Vertex vertex, GenericLocation location) {
        return vertex instanceof TransitStopVertex ?
                mapTransitVertexToPlace((TransitStopVertex) vertex) :
                mapLocationToPlace(location);
    }

    private Place mapLocationToPlace(GenericLocation location) {
        if (location.label == null || location.label.isEmpty()) {
            return new Place(location.lat, location.lng, String.format("%.6f, %.6f", location.lat, location.lng));
        } else {
            return new Place(location.lat, location.lng, location.label);
        }
    }

    private Place mapTransitVertexToPlace(TransitStopVertex vertex) {
        return mapStopToPlace(vertex.getStop(), null);
    }

    private Place mapStopToPlace(Stop stop, Integer stopIndex) {
        Place place = new Place(stop.getLat(), stop.getLon(), stop.getName());
        place.stopId = stop.getId();
        place.stopCode = stop.getCode();
        place.stopIndex = stopIndex;
        place.platformCode = stop.getCode();
        place.zoneId = stop.getZone();
        place.vertexType = VertexType.TRANSIT;
        return place;
    }

    private Calendar createCalendar(int timeInSeconds) {
        ZonedDateTime zdt = startOfTime.plusSeconds(timeInSeconds);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone(zdt.getZone()));
        c.setTimeInMillis(zdt.toInstant().toEpochMilli());
        return c;
    }

    private List<StopArrival> extractIntermediateStops(TransitPathLeg<TripSchedule> pathLeg) {
        List<StopArrival> visits = new ArrayList<>();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
        TripSchedule tripSchedule = pathLeg.trip();
        boolean boarded = false;

        for (int j = 0; j < tripPattern.stopPattern.stops.length; j++) {
            if (boarded && tripSchedule.arrival(j) == pathLeg.toTime()) {
                break;
            }
            if (boarded) {
                Stop stop = tripPattern.stopPattern.stops[j];
                Place place = mapStopToPlace(stop, j);
                // TODO: fill out stopSequence
                StopArrival visit = new StopArrival(
                        place,
                        createCalendar(tripSchedule.arrival(j)),
                        createCalendar(tripSchedule.departure(j))
                );
                visits.add(visit);
            }
            if (!boarded && tripSchedule.departure(j) == pathLeg.fromTime()) {
                boarded = true;
            }
        }
        return visits;
    }

    private List<Coordinate> extractTransitLegCoordinates(TransitPathLeg<TripSchedule> pathLeg) {
        List<Coordinate> transitLegCoordinates = new ArrayList<>();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
        TripSchedule tripSchedule = pathLeg.trip();
        boolean boarded = false;
        for (int j = 0; j < tripPattern.stopPattern.stops.length; j++) {
            int currentStopIndex = transitLayer.getStopIndex().indexByStop.get(tripPattern.getStop(j));
            if (boarded) {
                transitLegCoordinates.addAll(Arrays.asList(tripPattern.getHopGeometry(j - 1).getCoordinates()));
            }
            if (!boarded && tripSchedule.departure(j) == pathLeg.fromTime() && currentStopIndex == pathLeg.fromStop()) {
                boarded = true;
            }
            if (boarded && tripSchedule.arrival(j) == pathLeg.toTime() && currentStopIndex == pathLeg.toStop()) {
                break;
            }
        }
        return transitLegCoordinates;
    }

    private double getDistanceFromCoordinates(List<Coordinate> coordinates) {
        double distance = 0;
        for (int i = 1; i < coordinates.size(); i++) {
            distance += SphericalDistanceLibrary.distance(coordinates.get(i), coordinates.get(i - 1));
        }
        return distance;
    }
}
