package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.routing.algorithm.raptor.transit.cost.RaptorCostConverter.toOtpDomainCost;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.request.TransferWithDuration;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.util.PolylineEncoder;

/**
 * This maps the paths found by the Raptor search algorithm to the itinerary structure currently used by OTP. The paths,
 * access/egress transfers and transit layer only contains the minimal information needed for routing. Additional
 * information has to be fetched from the graph index to create complete itineraries that can be shown in a trip
 * planner.
 */
public class RaptorPathToItineraryMapper {

    private final Graph graph;

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
            Graph graph,
            TransitLayer transitLayer,
            ZonedDateTime startOfTime,
            RoutingRequest request
    ) {

        this.graph = graph;
        this.transitLayer = transitLayer;
        this.startOfTime = startOfTime;
        this.request = request;
    }

    public Itinerary createItinerary(Path<TripSchedule> path) {
        var optimizedPath = path instanceof OptimizedPath
                ? (OptimizedPath<TripSchedule>) path : null;
        List<Leg> legs = new ArrayList<>();

        // Map access leg
        legs.addAll(mapAccessLeg(path.accessLeg()));

        PathLeg<TripSchedule> pathLeg = path.accessLeg().nextLeg();

        boolean firstLeg = true;
        Leg transitLeg = null;

        while (!pathLeg.isEgressLeg()) {
            // Map transit leg
            if (pathLeg.isTransitLeg()) {
                transitLeg = mapTransitLeg(
                        request, transitLeg, pathLeg.asTransitLeg(), firstLeg
                );
                firstLeg = false;
                legs.add(transitLeg);
            }
            // Map transfer leg
            else if (pathLeg.isTransferLeg()) {
                legs.addAll(mapTransferLeg(
                        pathLeg.asTransferLeg(),
                        request.modes.transferMode == StreetMode.BIKE ? TraverseMode.BICYCLE : TraverseMode.WALK
                ));
            }

            pathLeg = pathLeg.nextLeg();
        }

        // Map egress leg
        EgressPathLeg<TripSchedule> egressPathLeg = pathLeg.asEgressLeg();
        Itinerary mapped = mapEgressLeg(egressPathLeg);
        legs.addAll(mapped == null ? List.of() : mapped.legs);
        propagateStopPlaceNamesToWalkingLegs(legs);

        Itinerary itinerary = new Itinerary(legs);

        // Map general itinerary fields
        itinerary.generalizedCost = toOtpDomainCost(path.generalizedCost());
        itinerary.arrivedAtDestinationWithRentedVehicle = mapped != null && mapped.arrivedAtDestinationWithRentedVehicle;

        if(optimizedPath != null) {
            itinerary.waitTimeOptimizedCost = toOtpDomainCost(optimizedPath.waitTimeOptimizedCost());
            itinerary.transferPriorityCost = toOtpDomainCost(optimizedPath.transferPriorityCost());
        }

        return itinerary;
    }

    private List<Leg> mapAccessLeg(AccessPathLeg<TripSchedule> accessPathLeg) {
        AccessEgress accessPath = (AccessEgress) accessPathLeg.access();

        if (accessPath.durationInSeconds() == 0) { return List.of(); }

        GraphPath graphPath = new GraphPath(accessPath.getLastState());

        Itinerary subItinerary = GraphPathToItineraryMapper
            .generateItinerary(graphPath, request.locale);

        if (subItinerary.legs.isEmpty()) { return List.of(); }

        subItinerary.timeShiftToStartAt(createCalendar(accessPathLeg.fromTime()));

        return subItinerary.legs;
    }

    private Leg mapTransitLeg(
            RoutingRequest request,
            Leg prevTransitLeg,
            TransitPathLeg<TripSchedule> pathLeg,
            boolean firstLeg
    ) {

        var boardStop = transitLayer.getStopByIndex(pathLeg.fromStop());
        var alightStop = transitLayer.getStopByIndex(pathLeg.toStop());
        TripSchedule tripSchedule = pathLeg.trip();
        TripTimes tripTimes = tripSchedule.getOriginalTripTimes();

        Leg leg = new Leg(tripTimes.getTrip());

        // Find stop positions in pattern where this leg boards and alights.
        // We cannot assume every stop appears only once in a pattern, so we
        // have to match stop and time.
        int boardStopIndexInPattern = tripSchedule.findDepartureStopPosition(
            pathLeg.fromTime(), pathLeg.fromStop()
        );
        int alightStopIndexInPattern = tripSchedule.findArrivalStopPosition(
            pathLeg.toTime(), pathLeg.toStop()
        );

        // Include real-time information in the Leg.
        if (!tripTimes.isScheduled()) {
            leg.setRealTime(true);
            leg.setDepartureDelay(tripTimes.getDepartureDelay(boardStopIndexInPattern));
            leg.setArrivalDelay(tripTimes.getArrivalDelay(alightStopIndexInPattern));
        }

        leg.setServiceDate(new ServiceDate(tripSchedule.getServiceDate()));
        leg.setStartTime(createCalendar(pathLeg.fromTime()));
        leg.setEndTime(createCalendar(pathLeg.toTime()));
        leg.setFrom(Place.forStop(boardStop));
        leg.setTo(Place.forStop(alightStop));
        List<Coordinate> transitLegCoordinates = extractTransitLegCoordinates(pathLeg, boardStopIndexInPattern, alightStopIndexInPattern);
        leg.setLegGeometry(PolylineEncoder.createEncodings(transitLegCoordinates));
        leg.setDistanceMeters(getDistanceFromCoordinates(transitLegCoordinates));

        // intermediate stops are required for fare calculation that's why we always add them here
        // and remove them in the API layers after the fare calculation if they were not requested.
        leg.setIntermediateStops(
                extractIntermediateStops(pathLeg, boardStopIndexInPattern, alightStopIndexInPattern));

        leg.setHeadsign(tripTimes.getHeadsign(boardStopIndexInPattern));
        leg.setWalkSteps(new ArrayList<>());
        leg.setGeneralizedCost(toOtpDomainCost(pathLeg.generalizedCost()));

        leg.setDropOffBookingInfo(tripTimes.getDropOffBookingInfo(boardStopIndexInPattern));
        leg.setPickupBookingInfo(tripTimes.getPickupBookingInfo(boardStopIndexInPattern));

        leg.setTransferToNextLeg((ConstrainedTransfer) pathLeg.getConstrainedTransferAfterLeg());
        leg.setTransferFromPrevLeg(
                prevTransitLeg == null ? null : prevTransitLeg.getTransferToNextLeg());

        leg.setBoardStopPosInPattern(boardStopIndexInPattern);
        leg.setAlightStopPosInPattern(alightStopIndexInPattern);

        leg.setBoardingGtfsStopSequence(tripTimes.getOriginalGtfsStopSequence(boardStopIndexInPattern));
        leg.setAlightGtfsStopSequence(tripTimes.getOriginalGtfsStopSequence(alightStopIndexInPattern));

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

        AlertToLegMapper.addTransitAlertPatchesToLeg(
            graph,
            leg,
            firstLeg,
            request.locale
        );

        return leg;
    }

    private List<Leg> mapTransferLeg(TransferPathLeg<TripSchedule> pathLeg, TraverseMode transferMode) {
        var transferFromStop = transitLayer.getStopByIndex(pathLeg.fromStop());
        var transferToStop = transitLayer.getStopByIndex(pathLeg.toStop());
        Transfer transfer = ((TransferWithDuration) pathLeg.transfer()).transfer();

        Place from = Place.forStop(transferFromStop);
        Place to = Place.forStop(transferToStop);
        return mapNonTransitLeg(pathLeg, transfer, transferMode, from, to);
    }

    private Itinerary mapEgressLeg(EgressPathLeg<TripSchedule> egressPathLeg) {
        AccessEgress egressPath = (AccessEgress) egressPathLeg.egress();

        if (egressPath.durationInSeconds() == 0) { return null; }

        GraphPath graphPath = new GraphPath(egressPath.getLastState());

        Itinerary subItinerary = GraphPathToItineraryMapper
            .generateItinerary(graphPath, request.locale);

        if (subItinerary.legs.isEmpty()) { return null; }

        subItinerary.timeShiftToStartAt(createCalendar(egressPathLeg.fromTime()));

        return subItinerary;
    }

    private List<Leg> mapNonTransitLeg(
            PathLeg<TripSchedule> pathLeg,
            Transfer transfer,
            TraverseMode transferMode,
            Place from,
            Place to
    ) {
        List<Edge> edges = transfer.getEdges();
        if (edges == null || edges.isEmpty()) {
            Leg leg = new Leg(transferMode);
            leg.setFrom(from);
            leg.setTo(to);
            leg.setStartTime(createCalendar(pathLeg.fromTime()));
            leg.setEndTime(createCalendar(pathLeg.toTime()));
            leg.setLegGeometry(PolylineEncoder.createEncodings(transfer.getCoordinates()));
            leg.setDistanceMeters((double) transfer.getDistanceMeters());
            leg.setWalkSteps(Collections.emptyList());
            leg.setGeneralizedCost(toOtpDomainCost(pathLeg.generalizedCost()));

            return List.of(leg);
        } else {
            // A RoutingRequest with a RoutingContext must be constructed so that the edges
            // may be re-traversed to create the leg(s) from the list of edges.
            try (RoutingRequest traverseRequest = Transfer.prepareTransferRoutingRequest(request)) {
                traverseRequest.setRoutingContext(graph, (Vertex) null, null);
                traverseRequest.arriveBy = false;

                StateEditor se = new StateEditor(traverseRequest, edges.get(0).getFromVertex());
                se.setTimeSeconds(createCalendar(pathLeg.fromTime()).getTimeInMillis() / 1000);

                State s = se.makeState();
                ArrayList<State> transferStates = new ArrayList<>();
                transferStates.add(s);
                for (Edge e : edges) {
                    s = e.traverse(s);
                    transferStates.add(s);
                }

                State[] states = transferStates.toArray(new State[0]);
                GraphPath graphPath = new GraphPath(states[states.length - 1]);

                Itinerary subItinerary = GraphPathToItineraryMapper
                        .generateItinerary(graphPath, request.locale);

                if (subItinerary.legs.isEmpty()) {
                    return List.of();
                }

                return subItinerary.legs;
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
                nextLeg.setFrom(currLeg.getTo());
            }

            if (!currLeg.isTransitLeg() && nextLeg.isTransitLeg()) {
                currLeg.setTo(nextLeg.getFrom());
            }
        }
    }

    private Calendar createCalendar(int timeInSeconds) {
        ZonedDateTime zdt = startOfTime.plusSeconds(timeInSeconds);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone(zdt.getZone()));
        c.setTimeInMillis(zdt.toInstant().toEpochMilli());
        return c;
    }

    private List<StopArrival> extractIntermediateStops(TransitPathLeg<TripSchedule> pathLeg, int boardStopIndexInPattern, int alightStopIndexInPattern) {
        List<StopArrival> visits = new ArrayList<>();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();
        TripSchedule tripSchedule = pathLeg.trip();

        for (int i = boardStopIndexInPattern + 1; i < alightStopIndexInPattern; i++) {
            var stop = tripPattern.getStop(i);

            StopArrival visit = new StopArrival(
                Place.forStop(stop),
                createCalendar(tripSchedule.arrival(i)),
                createCalendar(tripSchedule.departure(i)),
                i,
                tripSchedule.getOriginalTripTimes().getOriginalGtfsStopSequence(i)
            );
            visits.add(visit);
        }
        return visits;
    }

    private List<Coordinate> extractTransitLegCoordinates(TransitPathLeg<TripSchedule> pathLeg, int boardStopIndexInPattern, int alightStopIndexInPattern) {
        List<Coordinate> transitLegCoordinates = new ArrayList<>();
        TripPattern tripPattern = pathLeg.trip().getOriginalTripPattern();

        for (int i = boardStopIndexInPattern + 1; i <= alightStopIndexInPattern; i++) {
            transitLegCoordinates.addAll(Arrays.asList(tripPattern.getHopGeometry(i - 1).getCoordinates()));
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
