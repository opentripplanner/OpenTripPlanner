package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter.toOtpDomainCost;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.plan.FrequencyTransitLeg;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TransferWithDuration;
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
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;

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

    private final ZonedDateTime transitSearchTimeZero;


    /**
     * Constructs an itinerary mapper for a request and a set of results
     *
     * @param transitLayer the currently active transit layer (may have real-time data applied)
     * @param transitSearchTimeZero the point in time all times in seconds are counted from
     * @param request the current routing request
     */
    public RaptorPathToItineraryMapper(
            Graph graph,
            TransitLayer transitLayer,
            ZonedDateTime transitSearchTimeZero,
            RoutingRequest request
    ) {

        this.graph = graph;
        this.transitLayer = transitLayer;
        this.transitSearchTimeZero = transitSearchTimeZero;
        this.request = request;
    }

    public Itinerary createItinerary(Path<TripSchedule> path) {
        var optimizedPath = path instanceof OptimizedPath
                ? (OptimizedPath<TripSchedule>) path : null;

        // Map access leg
        List<Leg> legs = new ArrayList<>(mapAccessLeg(path.accessLeg()));

        PathLeg<TripSchedule> pathLeg = path.accessLeg().nextLeg();

        boolean firstLeg = true;
        Leg transitLeg = null;

        while (!pathLeg.isEgressLeg()) {
            // Map transit leg
            if (pathLeg.isTransitLeg()) {
                transitLeg = mapTransitLeg(transitLeg, pathLeg.asTransitLeg(), firstLeg);
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

        Itinerary itinerary = new Itinerary(legs);

        // Map general itinerary fields
        itinerary.generalizedCost = toOtpDomainCost(path.generalizedCost());
        itinerary.arrivedAtDestinationWithRentedVehicle = mapped != null && mapped.arrivedAtDestinationWithRentedVehicle;

        if(optimizedPath != null) {
            itinerary.waitTimeOptimizedCost = toOtpDomainCost(optimizedPath.generalizedCostWaitTimeOptimized());
            itinerary.transferPriorityCost = toOtpDomainCost(optimizedPath.transferPriorityCost());
        }

        return itinerary;
    }

    private List<Leg> mapAccessLeg(AccessPathLeg<TripSchedule> accessPathLeg) {
        AccessEgress accessPath = (AccessEgress) accessPathLeg.access();

        if (accessPath.durationInSeconds() == 0) { return List.of(); }

        GraphPath graphPath = new GraphPath(accessPath.getLastState());

        Itinerary subItinerary = GraphPathToItineraryMapper.generateItinerary(graphPath);

        if (subItinerary.legs.isEmpty()) { return List.of(); }

        subItinerary.timeShiftToStartAt(createCalendar(accessPathLeg.fromTime()));

        return subItinerary.legs;
    }

    private Leg mapTransitLeg(
            Leg prevTransitLeg,
            TransitPathLeg<TripSchedule> pathLeg,
            boolean firstLeg
    ) {
        TripSchedule tripSchedule = pathLeg.trip();

        // Find stop positions in pattern where this leg boards and alights.
        // We cannot assume every stop appears only once in a pattern, so we
        // have to match stop and time.
        int boardStopIndexInPattern = tripSchedule.findDepartureStopPosition(
            pathLeg.fromTime(), pathLeg.fromStop()
        );
        int alightStopIndexInPattern = tripSchedule.findArrivalStopPosition(
            pathLeg.toTime(), pathLeg.toStop()
        );

        Leg leg;
        if (tripSchedule.isFrequencyBasedTrip()) {
            int frequencyHeadwayInSeconds = tripSchedule.frequencyHeadwayInSeconds();
            leg = new FrequencyTransitLeg(
                    tripSchedule.getOriginalTripTimes(),
                    tripSchedule.getOriginalTripPattern(),
                    boardStopIndexInPattern,
                    alightStopIndexInPattern,
                    createCalendar(pathLeg.fromTime() + frequencyHeadwayInSeconds),
                    createCalendar(pathLeg.toTime()),
                    tripSchedule.getServiceDate(),
                    transitSearchTimeZero.getZone().normalized(),
                    (prevTransitLeg == null ? null : prevTransitLeg.getTransferToNextLeg()),
                    (ConstrainedTransfer) pathLeg.getConstrainedTransferAfterLeg(),
                    toOtpDomainCost(pathLeg.generalizedCost()),
                    frequencyHeadwayInSeconds
            );
        } else {
            leg = new ScheduledTransitLeg(
                    tripSchedule.getOriginalTripTimes(),
                    tripSchedule.getOriginalTripPattern(),
                    boardStopIndexInPattern,
                    alightStopIndexInPattern,
                    createCalendar(pathLeg.fromTime()),
                    createCalendar(pathLeg.toTime()),
                    tripSchedule.getServiceDate(),
                    transitSearchTimeZero.getZone().normalized(),
                    (prevTransitLeg == null ? null : prevTransitLeg.getTransferToNextLeg()),
                    (ConstrainedTransfer) pathLeg.getConstrainedTransferAfterLeg(),
                    toOtpDomainCost(pathLeg.generalizedCost())
            );
        }

        AlertToLegMapper.addTransitAlertPatchesToLeg(
            graph,
            leg,
            firstLeg
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

        Itinerary subItinerary = GraphPathToItineraryMapper.generateItinerary(graphPath);

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
            return List.of(new StreetLeg(
                    transferMode,
                    createCalendar(pathLeg.fromTime()),
                    createCalendar(pathLeg.toTime()),
                    from,
                    to,
                    (double) transfer.getDistanceMeters(),
                    toOtpDomainCost(pathLeg.generalizedCost()),
                    GeometryUtils.makeLineString(transfer.getCoordinates()),
                    List.of()
            ));
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

                Itinerary subItinerary = GraphPathToItineraryMapper.generateItinerary(graphPath);

                if (subItinerary.legs.isEmpty()) {
                    return List.of();
                }

                return subItinerary.legs;
            }
        }
    }

    private Calendar createCalendar(int timeInSeconds) {
        ZonedDateTime zdt = transitSearchTimeZero.plusSeconds(timeInSeconds);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone(zdt.getZone()));
        c.setTimeInMillis(zdt.toInstant().toEpochMilli());
        return c;
    }
}
