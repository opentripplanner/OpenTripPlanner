package org.opentripplanner.graph_builder.module.osm;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
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
import static org.opentripplanner.graph_builder.module.FakeGraph.*;

/**
 * Tests for planning with intermediate places
 */
public class TestIntermediatePlaces {

    public static final double DELTA = 0.001;

    private static TimeZone timeZone;

    private static GraphPathFinder graphPathFinder;

    @BeforeClass public static void setUp() {
        try {
            Graph graph = buildGraphNoTransit();
            addTransit(graph);
            link(graph);
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

        handleRequest(fromLocation, toLocation, intermediateLocations);
    }

    @Test public void testOneIntermediatePlace() {
        GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
        GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
        GenericLocation[] intermediateLocations = { new GenericLocation(39.92099, -82.95570) };

        handleRequest(fromLocation, toLocation, intermediateLocations);
    }

    @Test public void testTwoIntermediatePlaces() {
        GenericLocation fromLocation = new GenericLocation(39.93080, -82.98522);
        GenericLocation toLocation = new GenericLocation(39.96383, -82.96291);
        GenericLocation[] intermediateLocations = new GenericLocation[2];
        intermediateLocations[0] = new GenericLocation(39.92099, -82.95570);
        intermediateLocations[1] = new GenericLocation(39.96146, -82.99552);

        handleRequest(fromLocation, toLocation, intermediateLocations);
    }

    private void handleRequest(GenericLocation from, GenericLocation to, GenericLocation[] via) {
        RoutingRequest request = new RoutingRequest("WALK");
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
        assertEquals(1, plan.itinerary.size());
        Itinerary itinerary = plan.itinerary.get(0);
        assertEquals(1 + via.length, itinerary.legs.size());
        validateLegsTemporally(request, itinerary);
        validateLegsSpatially(plan, itinerary);
    }

    private void validateLegsSpatially(TripPlan plan, Itinerary itinerary) {
        Place place = plan.from;
        for (Leg leg : itinerary.legs) {
            assertPlacesAreEqual(place, leg.from);
            place = leg.to;
        }
        assertPlacesAreEqual(place, plan.to);
    }

    private void validateLegsTemporally(RoutingRequest request, Itinerary itinerary) {
        Calendar departTime = Calendar.getInstance(timeZone);
        Calendar arriveTime = Calendar.getInstance(timeZone);
        if (request.arriveBy) {
            departTime.setTimeInMillis(0);
            arriveTime.setTimeInMillis(request.dateTime);
        } else {
            departTime.setTimeInMillis(request.dateTime);
            arriveTime.setTimeInMillis(Long.MAX_VALUE);
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
        assertFalse(departTime.after(arriveTime));
        assertEquals(sumOfDuration, itinerary.duration.longValue());
    }

    private void assertLocationIsVeryCloseToPlace(GenericLocation location, Place place) {
        assertEquals(location.lat, place.lat, DELTA);
        assertEquals(location.lng, place.lon, DELTA);
    }

    private void assertPlacesAreEqual(Place a, Place b) {
        assertEquals(a.name, b.name);
        assertEquals(a.lat, b.lat, DELTA);
        assertEquals(a.lon, b.lon, DELTA);
    }
}
