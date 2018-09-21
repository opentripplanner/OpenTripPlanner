package org.opentripplanner.routing.flex;

import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.BoardAlightType;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;

import java.util.List;

import static org.junit.Assert.*;

public class VermontFlexRoutingTest {

    private Graph graph = ConstantsForTests.getInstance().getVermontGraph();
    private static final int MAX_WALK_DISTANCE = 804;
    private static final int MAX_WALK_DISTANCE_HEURISTIC = 5000;
    private static final double CALL_AND_RIDE_RELUCTANCE = 3.0;
    private static final double WALK_RELUCTANCE = 3.0;
    private static final double WAIT_AT_BEGINNING_FACTOR = 0;
    private static final int TRANSFER_PENALTY = 600;
    private static final boolean IGNORE_DRT_ADVANCE_MIN_BOOKING = true;

    // http://otp-vtrans-qa.camsys-apps.com/local/#plan?fromPlace=44.42145960616986%2C-72.01937198638917&toPlace=44.427773287332464%2C-72.0120351442025&date=05%2F23%2F2018&time=1%3A37%20PM&mode=TRANSIT%2CWALK&numItineraries=3&wheelchairAccessible=false&flagStopBufferSize=50&useReservationServices=true&useEligibilityServices=true
    // Flag stop on both sides, on Jay-Lyn (route 1382)
    @Test
    public void testFlagStop() {
        GraphPath path = getPathToDestination(
                buildRequest("44.4214596,-72.019371", "44.4277732,-72.01203514",
                "2018-05-23", "1:37pm")
        );
        List<Ride> rides = Ride.createRides(path);
        assertEquals(1, rides.size());
        Ride ride = rides.get(0);
        assertEquals("1382", ride.getRoute().getId());
        assertEquals(BoardAlightType.FLAG_STOP, ride.getBoardType());
        assertEquals(BoardAlightType.FLAG_STOP, ride.getAlightType());
        checkFare(path);
    }


    // http://otp-vtrans-qa.camsys-apps.com/local/#plan?fromPlace=44.38485134435363%2C-72.05881118774415&toPlace=44.422379116722084%2C-72.0198440551758&date=05%2F23%2F2018&time=1%3A37%20PM&arriveBy=false&maxWalkDistance=804&mode=TRANSIT%2CWALK&numItineraries=3&wheelchairAccessible=false&flagStopBufferSize=50&useReservationServices=true&useEligibilityServices=true
    // Deviated Route on both ends
    @Test
    public void testCallAndRide() {
        GraphPath path = getPathToDestination(
                buildRequest("44.38485134435363,-72.05881118774415", "44.422379116722084,-72.0198440551758",
                "2018-05-23", "1:37pm")
        );
        List<Ride> rides = Ride.createRides(path);
        assertEquals(1, rides.size());
        Ride ride = rides.get(0);
        assertEquals("7415", ride.getRoute().getId());
        assertEquals(BoardAlightType.DEVIATED, ride.getBoardType());
        assertEquals(BoardAlightType.DEVIATED, ride.getBoardType());
        checkFare(path);
    }

    //http://otp-vtrans-qa.camsys-apps.com/local/#plan?fromPlace=44.950950106914135%2C-72.20008850097658&toPlace=44.94985671536269%2C-72.13708877563478&date=06%2F11%2F2018&time=4%3A00%20PM&arriveBy=false&maxWalkDistance=804&mode=TRANSIT%2CWALK&numItineraries=3&wheelchairAccessible=false&flagStopBufferSize=50&useReservationServices=true&useEligibilityServices=true&=
    // Deviated Fixed Route at both ends
    @Test
    public void testDeviatedFixedRoute() {
        GraphPath path = getPathToDestination(
                buildRequest("44.950950106914135,-72.20008850097658", "44.94985671536269,-72.13708877563478",
                        "2018-05-23", "4:00pm")
        );
        List<Ride> rides = Ride.createRides(path);
        assertEquals(1, rides.size());
        Ride ride = rides.get(0);
        assertEquals("1383", ride.getRoute().getId());
        assertEquals(BoardAlightType.DEVIATED, ride.getBoardType());
        assertEquals(BoardAlightType.DEVIATED, ride.getAlightType());
        checkFare(path);
    }

    // http://otp-vtrans-qa.camsys-apps.com/local/#plan?fromPlace=44.8091683%2C-72.20580269999999&toPlace=44.94985671536269%2C-72.13708877563478&date=06%2F11%2F2018&time=4%3A00%20PM&arriveBy=false&maxWalkDistance=804&mode=TRANSIT%2CWALK&numItineraries=3&flagStopBufferSize=50&useReservationServices=true&useEligibilityServices=true
    // Flag stop to a deviated fixed route that starts as a regular route and ends deviated
    @Test
    public void testFlagStopToRegularStopEndingInDeviatedFixedRoute() {
        GraphPath path = getPathToDestination(
                buildRequest("44.8091683,-72.20580269999999", "44.94985671536269,-72.13708877563478",
                        "2018-06-13", "9:30am")
        );
        List<Ride> rides = Ride.createRides(path);
        assertEquals(2, rides.size());
        Ride ride = rides.get(0);
        assertEquals("3116", ride.getRoute().getId());
        assertEquals(BoardAlightType.FLAG_STOP, ride.getBoardType());
        assertEquals(BoardAlightType.DEFAULT, ride.getAlightType());

        Ride ride2 = rides.get(1);
        assertEquals("1383", ride2.getRoute().getId());
        assertEquals(BoardAlightType.DEFAULT, ride2.getBoardType());
        assertEquals(BoardAlightType.DEVIATED, ride2.getAlightType());
        checkFare(path);
    }

    private RoutingRequest buildRequest(String from, String to, String date, String time)
    {
        RoutingRequest options = new RoutingRequest();
        // defaults in vermont router-config.json
        options.maxWalkDistance = MAX_WALK_DISTANCE;
        options.maxWalkDistanceHeuristic = MAX_WALK_DISTANCE_HEURISTIC;
        options.callAndRideReluctance = CALL_AND_RIDE_RELUCTANCE;
        options.walkReluctance = WALK_RELUCTANCE;
        options.waitAtBeginningFactor = WAIT_AT_BEGINNING_FACTOR;
        options.transferPenalty = TRANSFER_PENALTY;
        // for testing
        options.ignoreDrtAdvanceBookMin = IGNORE_DRT_ADVANCE_MIN_BOOKING;
        options.setDateTime(date, time, graph.getTimeZone());
        options.setFromString(from);
        options.setToString(to);
        options.setRoutingContext(graph);

        return buildRoutingRequest(from, to, date, time, MAX_WALK_DISTANCE, MAX_WALK_DISTANCE_HEURISTIC,
                CALL_AND_RIDE_RELUCTANCE, WALK_RELUCTANCE, WAIT_AT_BEGINNING_FACTOR,
                TRANSFER_PENALTY, IGNORE_DRT_ADVANCE_MIN_BOOKING);
    }

    private RoutingRequest buildRoutingRequest(String from, String to, String date, String time, int maxWalkDistance,
                                               int maxWalkDistanceHeuristic, double callAndRideReluctance,
                                               double walkReluctance,
                                               double waitAtBeginningFactor, int transferPenalty,
                                               boolean ignoreDrtAdvanceMinBooking) {
        RoutingRequest options = new RoutingRequest();
        // defaults in vermont router-config.json
        options.maxWalkDistance = maxWalkDistance;
        options.maxWalkDistanceHeuristic = maxWalkDistanceHeuristic;
        options.callAndRideReluctance = callAndRideReluctance;
        options.walkReluctance = walkReluctance;
        options.waitAtBeginningFactor = waitAtBeginningFactor;
        options.transferPenalty = transferPenalty;

        // for testing
        options.ignoreDrtAdvanceBookMin = ignoreDrtAdvanceMinBooking;

        options.setDateTime(date, time, graph.getTimeZone());
        options.setFromString(from);
        options.setToString(to);
        options.setRoutingContext(graph);

        return options;
    }


    private GraphPath getPathToDestination(RoutingRequest options) {
        // Simulate GraphPathFinder - run modifiers for graph based on request
        FlagStopGraphModifier svc1 = new FlagStopGraphModifier(graph);
        DeviatedRouteGraphModifier svc2 = new DeviatedRouteGraphModifier(graph);
        svc1.createForwardHops(options);
        svc2.createForwardHops(options);
        svc1.createBackwardHops(options);
        svc2.createBackwardHops(options);

        AStar astar = new AStar();
        astar.getShortestPathTree(options);
        return astar.getPathsToTarget().iterator().next();
    }

    private void checkFare(GraphPath path) {
        // test fare calculated correctly
        FareService fareService = graph.getService(FareService.class);
        Fare cost = fareService.getCost(path);
        assertNotNull(cost);
        assertEquals("920", cost.getDetails(Fare.FareType.regular).iterator().next().fareId.getId());
    }
}
