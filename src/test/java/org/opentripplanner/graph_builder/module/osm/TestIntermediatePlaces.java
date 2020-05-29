package org.opentripplanner.graph_builder.module.osm;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for planning with intermediate places
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class TestIntermediatePlaces {

    /**
     * The spatial deviation that we allow in degrees
     */
    public static final double DELTA = 0.005;

    private static TimeZone timeZone;

    private static GraphPathFinder graphPathFinder;

    @BeforeClass public static void setUp() {
        try {
            Graph graph = FakeGraph.buildGraphNoTransit();
            FakeGraph.addPerpendicularRoutes(graph);
            FakeGraph.link(graph);
            graph.index();
            Router router = new Router(graph, RouterConfig.DEFAULT);
            router.startup();
            TestIntermediatePlaces.graphPathFinder = new GraphPathFinder(router);
            timeZone = graph.getTimeZone();
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Could not add transit data: " + e.toString();
        }
    }

    @Test public void testWithoutIntermediatePlaces() {
        GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
        GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
        GenericLocation[] intermediateLocations = {};

        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.WALK), false);
        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.WALK), true);
    }

    @Test @Ignore public void testOneIntermediatePlace() {
        GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
        GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
        GenericLocation[] intermediateLocations = { new GenericLocation(39.92099, -82.95570) };

        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.WALK), false);
        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.WALK), true);
    }

    @Test @Ignore public void testTwoIntermediatePlaces() {
        GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
        GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
        GenericLocation[] intermediateLocations = new GenericLocation[2];
        intermediateLocations[0] = new GenericLocation(39.92099, -82.95570);
        intermediateLocations[1] = new GenericLocation(39.96146, -82.99552);

        handleRequest(fromLocation, toLocation, intermediateLocations, new TraverseModeSet(TraverseMode.CAR), false);
        handleRequest(fromLocation, toLocation, intermediateLocations, new TraverseModeSet(TraverseMode.CAR), true);
    }

    @Test public void testTransitWithoutIntermediatePlaces() {
        GenericLocation fromLocation = new GenericLocation(39.9308, -83.0118);
        GenericLocation toLocation = new GenericLocation(39.9998, -83.0198);
        GenericLocation[] intermediateLocations = {};

        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.TRANSIT, TraverseMode.WALK), false);
        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.TRANSIT, TraverseMode.WALK), true);
    }

    @Test public void testThreeBusStopPlaces() {
        GenericLocation fromLocation = new GenericLocation(39.9058, -83.1341);
        GenericLocation toLocation = new GenericLocation(39.9058, -82.8841);
        GenericLocation[] intermediateLocations = { new GenericLocation(39.9058, -82.9841) };

        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.TRANSIT), false);
        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.TRANSIT), true);
    }

    @Test public void testTransitOneIntermediatePlace() {
        GenericLocation fromLocation = new GenericLocation(39.9108, -83.0118);
        GenericLocation toLocation = new GenericLocation(39.9698, -83.0198);
        GenericLocation[] intermediateLocations = { new GenericLocation(39.9948, -83.0148) };

        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.TRANSIT, TraverseMode.WALK), false);
        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.TRANSIT, TraverseMode.WALK), true);
    }

    @Test public void testTransitTwoIntermediatePlaces() {
        GenericLocation fromLocation = new GenericLocation(39.9908, -83.0118);
        GenericLocation toLocation = new GenericLocation(39.9998, -83.0198);
        GenericLocation[] intermediateLocations = new GenericLocation[2];
        intermediateLocations[0] = new GenericLocation(40.0000, -82.900);
        intermediateLocations[1] = new GenericLocation(39.9100, -83.100);

        handleRequest(fromLocation, toLocation, intermediateLocations,  new TraverseModeSet(TraverseMode.TRANSIT, TraverseMode.WALK), false);
        handleRequest(fromLocation, toLocation, intermediateLocations, new TraverseModeSet(TraverseMode.TRANSIT, TraverseMode.WALK), true);
    }

    private void handleRequest(GenericLocation from, GenericLocation to, GenericLocation[] via,
        TraverseModeSet modes, boolean arriveBy) {
        RoutingRequest request = new RoutingRequest(modes);
        request.setDateTime("2016-04-20", "13:00", timeZone);
        request.setArriveBy(arriveBy);
        request.from = from;
        request.to = to;
        for (GenericLocation intermediateLocation : via) {
            request.addIntermediatePlace(intermediateLocation);
        }
        List<GraphPath> paths = graphPathFinder.graphPathFinderEntryPoint(request);

        assertNotNull(paths);
        assertFalse(paths.isEmpty());

        List<Itinerary> itineraries = GraphPathToItineraryMapper.mapItineraries(paths, request);
        TripPlan plan = TripPlanMapper.mapTripPlan(request, itineraries);
        assertLocationIsVeryCloseToPlace(from, plan.from);
        assertLocationIsVeryCloseToPlace(to, plan.to);
        assertTrue(1 <= plan.itineraries.size());
        for (Itinerary itinerary : plan.itineraries) {
            validateIntermediatePlacesVisited(itinerary, via);
            assertTrue(via.length < itinerary.legs.size());
            validateLegsTemporally(request, itinerary);
            validateLegsSpatially(plan, itinerary);
            if (modes.contains(TraverseMode.TRANSIT)) {
                assert itinerary.transitTimeSeconds > 0;
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
            } while (Math.abs(leg.to.coordinate.latitude() - location.lat) > DELTA
                || Math.abs(leg.to.coordinate.longitude() - location.lng) > DELTA);
        }
    }

    // Check that the end point of a leg is also the start point of the next leg
    private void validateLegsSpatially(TripPlan plan, Itinerary itinerary) {
        Place place = plan.from;
        for (Leg leg : itinerary.legs) {
            assertEquals(place.coordinate, leg.from.coordinate);
            place = leg.to;
        }
        assertEquals(place.coordinate, plan.to.coordinate);
    }

    // Check that the start time and end time of each leg are consistent
    private void validateLegsTemporally(RoutingRequest request, Itinerary itinerary) {
        Calendar departTime = Calendar.getInstance(timeZone);
        Calendar arriveTime = Calendar.getInstance(timeZone);
        if (request.arriveBy) {
            departTime = itinerary.legs.get(0).startTime;
            arriveTime.setTimeInMillis(request.dateTime * 1000);
        } else {
            departTime.setTimeInMillis(request.dateTime * 1000);
            arriveTime = itinerary.legs.get(itinerary.legs.size() - 1).endTime;
        }
        long sumOfDuration = 0;
        for (Leg leg : itinerary.legs) {
            assertFalse(departTime.after(leg.startTime));
            assertFalse(leg.startTime.after(leg.endTime));

            departTime = leg.endTime;
            sumOfDuration += leg.getDuration();
        }
        sumOfDuration += itinerary.waitingTimeSeconds;

        assertFalse(departTime.after(arriveTime));

        // Check the total duration of the legs,
        int accuracy = itinerary.legs.size(); // allow 1 second per leg for rounding errors
        assertEquals(sumOfDuration, itinerary.durationSeconds, accuracy);
    }

    private void assertLocationIsVeryCloseToPlace(GenericLocation location, Place place) {
        assertEquals(location.lat, place.coordinate.latitude(), DELTA);
        assertEquals(location.lng, place.coordinate.longitude(), DELTA);
    }
}
