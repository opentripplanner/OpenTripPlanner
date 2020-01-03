package org.opentripplanner.routing.algorithm.raptor.itinerary;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.VertexType;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.FareService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * This maps the paths found by the Raptor search algorithm to the itinerary structure currently used by OTP. The paths,
 * access/egress transfers and transit layer only contains the minimal information needed for routing. Additional
 * information has to be fetched from the graph index to create complete itineraries that can be shown in a trip
 * planner.
 */
public class ItineraryMapper {

    private final TransitLayer transitLayer;

    private final RoutingRequest request;

    private final ZonedDateTime startOfTime;

    private final Map<Stop, Transfer> accessTransfers;

    private final Map<Stop, Transfer> egressTransfers;

    private static Logger LOG = LoggerFactory.getLogger(ItineraryMapper.class);

    /**
     * Constructs an itinerary mapper for a request and a set of results
     *
     * @param transitLayer the currently active transit layer (may have real-time data applied)
     * @param startOfTime the point in time all times in seconds are counted from
     * @param request the current routing request
     * @param accessTransfers the access paths calculated for this request by access stop
     * @param egressTransfers the egress paths calculated for this request by egress stop
     */
    public ItineraryMapper(
            TransitLayer transitLayer,
            ZonedDateTime startOfTime,
            RoutingRequest request,
            Map<Stop, Transfer> accessTransfers,
            Map<Stop, Transfer> egressTransfers) {

        this.transitLayer = transitLayer;
        this.startOfTime = startOfTime;
        this.request = request;
        this.accessTransfers = accessTransfers;
        this.egressTransfers = egressTransfers;
    }

    public Itinerary createItinerary(Path<TripSchedule> path) {
        Itinerary itinerary = new Itinerary();

        // Map access leg
        mapAccessLeg(itinerary, path.accessLeg(), accessTransfers);

        // TODO: Add back this code when PathLeg interface contains object references

        PathLeg<TripSchedule> pathLeg = path.accessLeg().nextLeg();

        boolean firstLeg = true;

        while (!pathLeg.isEgressLeg()) {
            // Map transit leg
            if (pathLeg.isTransitLeg()) {
                Leg transitLeg = mapTransitLeg(request, pathLeg.asTransitLeg(), firstLeg);
                firstLeg = false;
                itinerary.addLeg(transitLeg);
                // Increment counters
                itinerary.transitTime += pathLeg.duration();
            }

            // Map transfer leg
            if (pathLeg.isTransferLeg()) {
                mapTransferLeg(itinerary, pathLeg.asTransferLeg());
            }

            pathLeg = pathLeg.nextLeg();
        }

        // Map egress leg
        EgressPathLeg<TripSchedule> egressPathLeg = pathLeg.asEgressLeg();
        mapEgressLeg(itinerary, egressPathLeg, egressTransfers);

        // Map general itinerary fields
        itinerary.transfers = path.numberOfTransfers();
        itinerary.startTime = createCalendar(path.accessLeg().fromTime());
        itinerary.endTime = createCalendar(egressPathLeg.toTime());
        itinerary.duration = (long) egressPathLeg.toTime() - path.accessLeg().fromTime();
        itinerary.waitingTime = itinerary.duration - itinerary.walkTime - itinerary.transitTime;
        itinerary.walkLimitExceeded = itinerary.walkDistance > request.maxWalkDistance;

        return itinerary;
    }

    private void mapAccessLeg(
            Itinerary itinerary,
            AccessPathLeg<TripSchedule> accessPathLeg,
            Map<Stop, Transfer> accessPaths
    ) {
        Stop accessToStop = transitLayer.getStopByIndex(accessPathLeg.toStop());
        Transfer accessPath = accessPaths.get(accessToStop);

        // TODO Need to account for multiple fromVertices
        Place from = mapOriginTargetToPlace(request.rctx.fromVertices.iterator().next(), request.from);
        Place to = mapStopToPlace(accessToStop);
        mapNonTransitLeg(itinerary, accessPathLeg, accessPath, from, to, true);
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

        leg.serviceDate = new ServiceDate(request.getDateTime()).getAsString(); // TODO: This has to be changed for multi-day searches
        leg.intermediateStops = new ArrayList<>();
        leg.startTime = createCalendar(pathLeg.fromTime());
        leg.endTime = createCalendar(pathLeg.toTime());
        leg.mode = tripPattern.mode.toString();
        leg.tripId = trip.getId();
        leg.from = mapStopToPlace(boardStop);
        leg.to = mapStopToPlace(alightStop);
        List<Coordinate> transitLegCoordinates = extractTransitLegCoordinates(pathLeg);
        leg.legGeometry = PolylineEncoder.createEncodings(transitLegCoordinates);
        leg.distance = getDistanceFromCoordinates(transitLegCoordinates);

        if (request.showIntermediateStops) {
            leg.intermediateStops = extractIntermediateStops(pathLeg);
        }

        leg.route = route.getLongName();
        leg.routeId = route.getId();
        leg.agencyName = route.getAgency().getName();
        leg.routeColor = route.getColor();
        leg.tripShortName = route.getShortName();
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

        GraphPathToTripPlanConverter.addAlertPatchesToLeg(
            request.getRoutingContext().graph,
            leg,
            firstLeg,
            request.locale
        );

        return leg;
    }

    private void mapTransferLeg(Itinerary itinerary, TransferPathLeg<TripSchedule> pathLeg) {
        Stop transferFromStop = transitLayer.getStopByIndex(pathLeg.fromStop());
        Stop transferToStop = transitLayer.getStopByIndex(pathLeg.toStop());
        Transfer transfer = transitLayer.getTransferByStopIndex().get(pathLeg.fromStop()).stream().filter(t -> t.getToStop() == pathLeg.toStop()).findFirst().get();

        Place from = mapStopToPlace(transferFromStop);
        Place to = mapStopToPlace(transferToStop);
        mapNonTransitLeg(itinerary, pathLeg, transfer, from, to, false);
    }

    private void mapEgressLeg(
            Itinerary itinerary,
            EgressPathLeg<TripSchedule> egressPathLeg,
            Map<Stop, Transfer> egressPaths
    ) {
        Stop egressStop = transitLayer.getStopByIndex(egressPathLeg.fromStop());
        Transfer egressPath = egressPaths.get(egressStop);

        Place from = mapStopToPlace(egressStop);
        // TODO Need to account for multiple toVertices
        Place to = mapOriginTargetToPlace(request.rctx.toVertices.iterator().next(), request.to);
        mapNonTransitLeg(itinerary, egressPathLeg, egressPath, from, to, true);
    }

    private void mapNonTransitLeg(Itinerary itinerary, PathLeg<TripSchedule> pathLeg, Transfer transfer, Place from, Place to, boolean onlyIfNonZeroDistance) {
        List<Edge> edges = transfer.getEdges();
        if (edges.isEmpty()) {
            Leg leg = new Leg();
            leg.from = from;
            leg.to = to;
            leg.startTime = createCalendar(pathLeg.fromTime());
            leg.endTime = createCalendar(pathLeg.toTime());
            leg.mode = TraverseMode.WALK.name();
            leg.legGeometry = PolylineEncoder.createEncodings(transfer.getCoordinates());
            leg.distance = (double) transfer.getDistanceMeters();
            leg.walkSteps = Collections.emptyList();

            if (!onlyIfNonZeroDistance || leg.distance > 0) {
                itinerary.walkTime += pathLeg.fromTime() - pathLeg.fromTime();
                itinerary.walkDistance += transfer.getDistanceMeters();
                itinerary.addLeg(leg);
            }
        } else {
            StateEditor se = new StateEditor(request, edges.get(0).getFromVertex());
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
                Itinerary subItinerary = GraphPathToTripPlanConverter
                        .generateItinerary(graphPath, false, true, request.locale);

                if (!onlyIfNonZeroDistance || subItinerary.walkDistance > 0) {
                    subItinerary.legs.forEach(leg -> {
                        itinerary.walkDistance += subItinerary.walkDistance;
                        itinerary.walkTime += subItinerary.walkTime;
                        itinerary.addLeg(leg);
                    });
                }
            } catch (TrivialPathException e) {
                // Ignore, no legs need be copied
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
            return new Place(location.lng, location.lat, String.format("%.6f, %.6f", location.lat, location.lng));
        } else {
            return new Place(location.lng, location.lat, location.label);
        }
    }

    private Place mapTransitVertexToPlace(TransitStopVertex vertex) {
        return mapStopToPlace(vertex.getStop());
    }

    private Place mapStopToPlace(Stop stop) {
        Place place = new Place(stop.getLon(), stop.getLat(), stop.getName());
        place.stopId = stop.getId();
        place.stopCode = stop.getCode();
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

    private List<Place> extractIntermediateStops(TransitPathLeg<TripSchedule> pathLeg) {
        List<Place> places = new ArrayList<>();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
        TripSchedule tripSchedule = pathLeg.trip();
        boolean boarded = false;
        for (int j = 0; j < tripPattern.stopPattern.stops.length; j++) {
            if (boarded && tripSchedule.arrival(j) == pathLeg.toTime()) {
                break;
            }
            if (boarded) {
                Stop stop = tripPattern.stopPattern.stops[j];
                Place place = mapStopToPlace(stop);
                place.stopIndex = j;
                // TODO: fill out stopSequence
                place.arrival = createCalendar(tripSchedule.arrival(j));
                place.departure = createCalendar(tripSchedule.departure(j));
                places.add(place);
            }
            if (!boarded && tripSchedule.departure(j) == pathLeg.fromTime()) {
                boarded = true;
            }
        }
        return places;
    }

    private List<Coordinate> extractTransitLegCoordinates(TransitPathLeg<TripSchedule> pathLeg) {
        List<Coordinate> transitLegCoordinates = new ArrayList<>();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
        TripSchedule tripSchedule = pathLeg.trip();
        boolean boarded = false;
        for (int j = 0; j < tripPattern.stopPattern.stops.length; j++) {
            if (boarded) {
                transitLegCoordinates.addAll(Arrays.asList(tripPattern.getHopGeometry(j - 1).getCoordinates()));
            }
            if (!boarded && tripSchedule.departure(j) == pathLeg.fromTime()) {
                boarded = true;
            }
            if (boarded && tripSchedule.arrival(j) == pathLeg.toTime()) {
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
