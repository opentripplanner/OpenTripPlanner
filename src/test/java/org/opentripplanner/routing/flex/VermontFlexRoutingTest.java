package org.opentripplanner.routing.flex;

import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.BoardAlightType;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.DateUtils;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class VermontFlexRoutingTest {

    private Graph graph = ConstantsForTests.getInstance().getVermontGraph();
    private static final int MAX_WALK_DISTANCE = 804;
    private static final double CALL_AND_RIDE_RELUCTANCE = 3.0;
    private static final double WALK_RELUCTANCE = 3.0;
    private static final double WAIT_AT_BEGINNING_FACTOR = 0;
    private static final int TRANSFER_PENALTY = 600;
    private static final boolean IGNORE_DRT_ADVANCE_MIN_BOOKING = true;

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

        // Check that times are respected. DAR is available 1pm-3pm

        // arriveBy=false Check start time respected. If request is for before 1pm, we should get a trip starting at 1pm.
        path = getPathToDestination(
                buildRequest("44.38485134435363,-72.05881118774415", "44.422379116722084,-72.0198440551758",
                        "2018-05-23", "12:50pm")
        );
        rides = Ride.createRides(path);
        assertEquals(1, rides.size());
        ride = rides.get(0);
        assertEquals("7415", ride.getRoute().getId());
        assertDateEquals(ride.getStartTime(), "2018-05-23", "1:00pm", graph.getTimeZone());

        // arriveBy=false Check end time respected.
        path = getPathToDestination(
                buildRequest("44.38485134435363,-72.05881118774415", "44.422379116722084,-72.0198440551758",
                        "2018-05-23", "2:56pm")
        );
        rides = Ride.createRides(path);
        assertEquals(1, rides.size());
        ride = rides.get(0);
        assertEquals("7415", ride.getRoute().getId());
        assertDateEquals(ride.getStartTime(), "2018-05-24", "1:00pm", graph.getTimeZone());

        // arriveBy=true Check start time respected.
        path = getPathToDestination(
                buildRequest("44.38485134435363,-72.05881118774415", "44.422379116722084,-72.0198440551758",
                        "2018-05-24", "1:05pm", true)
        );
        rides = Ride.createRides(path);
        assertEquals(1, rides.size());
        ride = rides.get(0);
        assertEquals("7415", ride.getRoute().getId());
        assertDateEquals(ride.getEndTime(), "2018-05-23", "3:00pm", graph.getTimeZone());

        // arriveBy=true Check end time respected
        path = getPathToDestination(
                buildRequest("44.38485134435363,-72.05881118774415", "44.422379116722084,-72.0198440551758",
                        "2018-05-24", "3:05pm", true)
        );
        rides = Ride.createRides(path);
        assertEquals(1, rides.size());
        ride = rides.get(0);
        assertEquals("7415", ride.getRoute().getId());
        assertDateEquals(ride.getEndTime(), "2018-05-24", "3:00pm", graph.getTimeZone());
    }

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

    private RoutingRequest buildRequest(String from, String to, String date, String time) {
        return buildRequest(from, to, date, time, false);
    }

    private RoutingRequest buildRequest(String from, String to, String date, String time, boolean arriveBy) {
        return buildRoutingRequest(from, to, date, time, arriveBy, MAX_WALK_DISTANCE,
                CALL_AND_RIDE_RELUCTANCE, WALK_RELUCTANCE, WAIT_AT_BEGINNING_FACTOR,
                TRANSFER_PENALTY, IGNORE_DRT_ADVANCE_MIN_BOOKING);
    }

    private RoutingRequest buildRoutingRequest(String from, String to, String date, String time, boolean arriveBy,
                                               int maxWalkDistance, double callAndRideReluctance, double walkReluctance,
                                               double waitAtBeginningFactor, int transferPenalty,
                                               boolean ignoreDrtAdvanceMinBooking) {
        RoutingRequest options = new RoutingRequest();
        options.setArriveBy(arriveBy);
        // defaults in vermont router-config.json
        options.setMaxWalkDistance(maxWalkDistance);
        options.flexCallAndRideReluctance = callAndRideReluctance;
        options.walkReluctance = walkReluctance;
        options.waitAtBeginningFactor = waitAtBeginningFactor;
        options.transferPenalty = transferPenalty;

        // for testing
        options.flexIgnoreDrtAdvanceBookMin = ignoreDrtAdvanceMinBooking;

        options.setDateTime(date, time, graph.getTimeZone());
        options.setFromString(from);
        options.setToString(to);
        options.setRoutingContext(graph);

        return options;
    }


    private GraphPath getPathToDestination(RoutingRequest options) {
        Router router = new Router("default", graph);
        return new GraphPathFinder(router).getPaths(options).get(0);
    }

    private void checkFare(GraphPath path) {
        // test fare calculated correctly
        FareService fareService = graph.getService(FareService.class);
        Fare cost = fareService.getCost(path);
        assertNotNull(cost);
        assertEquals("920", cost.getDetails(Fare.FareType.regular).iterator().next().fareId.getId());
    }

    private void assertDateEquals(long timeSeconds, String date, String time, TimeZone timeZone) {
        assertEquals(DateUtils.toDate(date, time, timeZone), new Date(timeSeconds * 1000));
    }
}
