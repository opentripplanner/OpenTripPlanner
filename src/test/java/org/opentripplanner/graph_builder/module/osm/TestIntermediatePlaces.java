package org.opentripplanner.graph_builder.module.osm;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.impl.MemoryGraphSource;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Tests for planning with intermediate places
 */
public class TestIntermediatePlaces {

    /**
     * The spacial deviation that we allow in degrees
     */
    public static final double DELTA = 0.005;

    private static TimeZone timeZone;

    private static GraphPathFinder graphPathFinder;

    @BeforeClass public static void setUp() {
        try {
            Graph graph = FakeGraph.buildGraphNoTransit();
            FakeGraph.addPerpendicularRoutes(graph);
            FakeGraph.link(graph);
            graph.index(new DefaultStreetVertexIndexFactory());

            OTPServer otpServer = new OTPServer(new CommandLineParameters(), new GraphService());
            otpServer.getGraphService().registerGraph("A", new MemoryGraphSource("A", graph));

            Router router = otpServer.getGraphService().getRouter("A");
            TestIntermediatePlaces.graphPathFinder = new GraphPathFinder(router);
            timeZone = graph.getTimeZone();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assert false : "Could not build graph: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Could not add transit data: " + e.getMessage();
        }
    }

    @Test public void testWithoutIntermediatePlaces() {
        GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
        GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
        GenericLocation[] intermediateLocations = {};

        handleRequest(fromLocation, toLocation, intermediateLocations, "WALK", false);
        handleRequest(fromLocation, toLocation, intermediateLocations, "WALK", true);
    }

    @Test @Ignore public void testOneIntermediatePlace() {
        GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
        GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
        GenericLocation[] intermediateLocations = { new GenericLocation(39.92099, -82.95570) };

        handleRequest(fromLocation, toLocation, intermediateLocations, "WALK", false);
        handleRequest(fromLocation, toLocation, intermediateLocations, "WALK", true);
    }

    @Test @Ignore public void testTwoIntermediatePlaces() {
        GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
        GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
        GenericLocation[] intermediateLocations = new GenericLocation[2];
        intermediateLocations[0] = new GenericLocation(39.92099, -82.95570);
        intermediateLocations[1] = new GenericLocation(39.96146, -82.99552);

        handleRequest(fromLocation, toLocation, intermediateLocations, "CAR", false);
        handleRequest(fromLocation, toLocation, intermediateLocations, "CAR", true);
    }

    @Test public void testTransitWithoutIntermediatePlaces() {
        GenericLocation fromLocation = new GenericLocation(39.9308, -83.0118);
        GenericLocation toLocation = new GenericLocation(39.9998, -83.0198);
        GenericLocation[] intermediateLocations = {};

        handleRequest(fromLocation, toLocation, intermediateLocations, "TRANSIT,WALK", false);
        handleRequest(fromLocation, toLocation, intermediateLocations, "TRANSIT,WALK", true);
    }

    @Test public void testThreeBusStopPlaces() {
        GenericLocation fromLocation = new GenericLocation(39.9058, -83.1341);
        GenericLocation toLocation = new GenericLocation(39.9058, -82.8841);
        GenericLocation[] intermediateLocations = { new GenericLocation(39.9058, -82.9841) };

        handleRequest(fromLocation, toLocation, intermediateLocations, "TRANSIT", false);
        handleRequest(fromLocation, toLocation, intermediateLocations, "TRANSIT", true);
    }

    @Test public void testTransitOneIntermediatePlace() {
        GenericLocation fromLocation = new GenericLocation(39.9108, -83.0118);
        GenericLocation toLocation = new GenericLocation(39.9698, -83.0198);
        GenericLocation[] intermediateLocations = { new GenericLocation(39.9948, -83.0148) };

        handleRequest(fromLocation, toLocation, intermediateLocations, "TRANSIT,WALK", false);
        handleRequest(fromLocation, toLocation, intermediateLocations, "TRANSIT,WALK", true);
    }

    @Test public void testTransitTwoIntermediatePlaces() {
        GenericLocation fromLocation = new GenericLocation(39.9908, -83.0118);
        GenericLocation toLocation = new GenericLocation(39.9998, -83.0198);
        GenericLocation[] intermediateLocations = new GenericLocation[2];
        intermediateLocations[0] = new GenericLocation(40.0000, -82.900);
        intermediateLocations[1] = new GenericLocation(39.9100, -83.100);

        handleRequest(fromLocation, toLocation, intermediateLocations, "TRANSIT,WALK", false);
        handleRequest(fromLocation, toLocation, intermediateLocations, "TRANSIT,WALK", true);
    }

    private void handleRequest(GenericLocation from, GenericLocation to, GenericLocation[] via,
        String modes, boolean arriveBy) {
        RoutingRequest request = new RoutingRequest(modes);
        request.setDateTime("2016-04-20", "13:00", timeZone);
        request.setArriveBy(arriveBy);
        request.from = from;
        request.to = to;
        for (GenericLocation intermediateLocation : via) {
            request.addIntermediatePlace(intermediateLocation);
        }
        List<GraphPath> pathList = graphPathFinder.graphPathFinderEntryPoint(request);

        assertNotNull(pathList);
        assertFalse(pathList.isEmpty());

        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(pathList, request);
        assertLocationIsVeryCloseToPlace(from, plan.from);
        assertLocationIsVeryCloseToPlace(to, plan.to);
        assertTrue(1 <= plan.itinerary.size());
        for (Itinerary itinerary : plan.itinerary) {
            validateIntermediatePlacesVisited(itinerary, via);
            assertTrue(via.length < itinerary.legs.size());
            validateLegsTemporally(request, itinerary);
            validateLegsSpatially(plan, itinerary);
            if (modes.contains("TRANSIT")) {
                assert itinerary.transitTime > 0;
            }
        }
    }

    // Check that every via location is visited in the right order
    private void validateIntermediatePlacesVisited(Itinerary itinerary, GenericLocation[] via) {
        int legIndex = 0;

        for (GenericLocation location : via) {
            Leg leg;
            do {
                assertTrue("Intermediate location was not an endpoint of any leg",
                    legIndex < itinerary.legs.size());
                leg = itinerary.legs.get(legIndex);
                legIndex++;
            } while (Math.abs(leg.to.lat - location.lat) > DELTA
                || Math.abs(leg.to.lon - location.lng) > DELTA);
        }
    }

    // Check that the end point of a leg is also the start point of the next leg
    private void validateLegsSpatially(TripPlan plan, Itinerary itinerary) {
        Place place = plan.from;
        for (Leg leg : itinerary.legs) {
            assertPlacesAreVeryClose(place, leg.from);
            place = leg.to;
        }
        assertPlacesAreVeryClose(place, plan.to);
    }

    // Check that the start time and end time of each leg are consistent
    private void validateLegsTemporally(RoutingRequest request, Itinerary itinerary) {
        Calendar departTime = Calendar.getInstance(timeZone);
        Calendar arriveTime = Calendar.getInstance(timeZone);
        if (request.arriveBy) {
            departTime = itinerary.legs.get(0).from.departure;
            arriveTime.setTimeInMillis(request.dateTime * 1000);
        } else {
            departTime.setTimeInMillis(request.dateTime * 1000);
            arriveTime = itinerary.legs.get(itinerary.legs.size() - 1).to.arrival;
        }
        long sumOfDuration = 0;
        for (Leg leg : itinerary.legs) {
            assertFalse(departTime.after(leg.startTime));
            assertEquals(leg.startTime, leg.from.departure);
            assertEquals(leg.endTime, leg.to.arrival);
            assertFalse(leg.startTime.after(leg.endTime));

            departTime = leg.to.arrival;
            sumOfDuration += leg.getDuration();
        }
        sumOfDuration += itinerary.waitingTime;

        assertFalse(departTime.after(arriveTime));

        // Check the total duration of the legs,
        int accuracy = itinerary.legs.size(); // allow 1 second per leg for rounding errors
        assertEquals(sumOfDuration, itinerary.duration.doubleValue(), accuracy);
    }

    private void assertLocationIsVeryCloseToPlace(GenericLocation location, Place place) {
        assertEquals(location.lat, place.lat, DELTA);
        assertEquals(location.lng, place.lon, DELTA);
    }

    private void assertPlacesAreVeryClose(Place a, Place b) {
        assertEquals(a.lat, b.lat, DELTA);
        assertEquals(a.lon, b.lon, DELTA);
    }
}
