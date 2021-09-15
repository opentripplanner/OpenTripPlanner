package org.opentripplanner.routing.core;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.PolylineAssert.assertThatPolylinesAreEqual;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;

public class AccessibilityRoutingTest {

    private static final float DELTA = 0.1f;

    static long dateTime = OffsetDateTime.parse("2021-09-14T10:15:29-04:00").toEpochSecond();

    static Graph graph = getDefaultGraph();

    public static Graph getDefaultGraph() {
        return ConstantsForTests.getGraph(ConstantsForTests.CENTRAL_ATLANTA_OSM, ConstantsForTests.ATLANTA_BLUE_GREEN_LINE);
    }

    @Test
    public void accessibleTrip() {
        // near Georgia State station
        GenericLocation start = new GenericLocation(33.75054, -84.38409);
        // near Ashby station
        GenericLocation end = new GenericLocation(33.75744, -84.41754);

        Itinerary i = getTripPlan(start, end).itinerary.get(0);
        List<Leg> transitLegs = i.legs.stream().filter(Leg::isTransitLeg).collect(Collectors.toList());

        assertEquals(1, transitLegs.size());

        Leg leg = transitLegs.get(0);
        assertEquals("BLUE", leg.routeShortName);

        assertEquals("GEORGIA STATE STATION", leg.from.name);
        assertEquals("ASHBY STATION", leg.to.name);
        // since both the start and end stops and the trip are accessible, we get a perfect score
        assertEquals(1, leg.accessibilityScore);
    }

    @Test
    public void tripWhereStartIsNotAccessible() {
        // near Five Points station
        GenericLocation start = new GenericLocation(33.75374, -84.39228);
        // near Ashby station
        GenericLocation end = new GenericLocation(33.75744, -84.41754);

        Itinerary i = getTripPlan(start, end).itinerary.get(0);
        List<Leg> transitLegs = i.legs.stream().filter(Leg::isTransitLeg).collect(Collectors.toList());

        assertEquals(1, transitLegs.size());

        Leg leg = transitLegs.get(0);
        assertEquals("BLUE", leg.routeShortName);

        assertEquals("FIVE POINTS STATION", leg.from.name);
        assertEquals("ASHBY STATION", leg.to.name);

        // the start is not accessible, so we get a lowered accessibility score
        assertEquals(0.666666f, leg.accessibilityScore, DELTA);


        // if we increase the cost of boarding at a stop which we know to be inaccessible, then
        // long walks will be chosen instead

        i = getTripPlan(start, end, r -> r.noWheelchairAccessAtStopPenalty = 10000).itinerary.get(0);
        transitLegs = i.legs.stream().filter(Leg::isTransitLeg).collect(Collectors.toList());

        leg = transitLegs.get(0);
        assertEquals("BLUE", leg.routeShortName);

        assertEquals("GEORGIA STATE STATION", leg.from.name);
        assertEquals("ASHBY STATION", leg.to.name);

        // because we are walking to a station with good accessibility, we get a perfect score again
        assertEquals(1, leg.accessibilityScore);

    }

    @Test
    public void tripWhereStartHasUnknownAccessibility() {
        // near CNN Center station
        GenericLocation start = new GenericLocation(33.7565, -84.3958);
        // near Ashby station
        GenericLocation end = new GenericLocation(33.75744, -84.41754);

        Itinerary i = getTripPlan(start, end).itinerary.get(0);

        List<Leg> transitLegs = i.legs.stream().filter(Leg::isTransitLeg).collect(Collectors.toList());

        assertEquals(1, transitLegs.size());

        Leg leg = transitLegs.get(0);
        assertEquals("BLUE", leg.routeShortName);

        assertEquals("GWCC-CNN CENTER STATION", leg.from.name);
        assertEquals("ASHBY STATION", leg.to.name);

        // the start has unknown access, so we get a lowered score
        assertEquals(0.833333, leg.accessibilityScore, DELTA);


        // if we increase the walking distance and the penalty another stop is used instead

        i = getTripPlan(start, end, r -> {
            r.unknownWheelchairAccessAtStopPenalty = 10000;
        }).itinerary.get(0);
        transitLegs = i.legs.stream().filter(Leg::isTransitLeg).collect(Collectors.toList());

        leg = transitLegs.get(0);
        assertEquals("BLUE", leg.routeShortName);

        assertEquals("FIVE POINTS STATION", leg.from.name);
        assertEquals("ASHBY STATION", leg.to.name);

        // because we are walking to a station with bad accessibility we get a lower score
        assertEquals(0.666f, leg.accessibilityScore, DELTA);

    }

    @Test
    public void badTripScore() {
        // near Georgia State station
        GenericLocation start = new GenericLocation(33.75054, -84.38409);
        // near Ashby station
        GenericLocation end = new GenericLocation(33.75744, -84.41754);

        // we are banning the blue line even though it is accessible to force getting on the green one
        Itinerary i = getTripPlan(start, end, r -> r.setBannedRoutes("1_BLUE")).itinerary.get(0);
        List<Leg> transitLegs = i.legs.stream().filter(Leg::isTransitLeg).collect(Collectors.toList());

        Leg leg = transitLegs.get(0);
        assertEquals("GREEN", leg.routeShortName);

        assertEquals("GEORGIA STATE STATION", leg.from.name);
        assertEquals("ASHBY STATION", leg.to.name);

        // because are using a trip that has unknown accessibility, it has a lower score
        assertEquals(0.8333f, leg.accessibilityScore, DELTA);

    }

    @Test
    public void stairsPenalty() {
        GenericLocation start = new GenericLocation(33.75630, -84.39527);
        GenericLocation end = new GenericLocation(33.75649, -84.39580);

        Itinerary i = getTripPlan(start, end, r -> r.setMode(TraverseMode.WALK)).itinerary.get(0);
        Leg leg = i.legs.get(0);
        assertEquals("WALK", leg.mode);
        assertThatPolylinesAreEqual("y_`mEnmbbObA}@U]{@wAIMEOMHwBz@^p@Zn@Zx@Ld@DJLd@?@", leg.legGeometry.getPoints());

        // if we reduce the stair penalty for wheelchairs we get a route that goes up the stairs
        i = getTripPlan(start, end, r -> {
            r.setMode(TraverseMode.WALK);
            r.wheelchairStairsPenalty = 0;
        }).itinerary.get(0);

        leg = i.legs.get(0);
        assertEquals("WALK", leg.mode);
        assertThatPolylinesAreEqual("y_`mEnmbbO[XIHBFUTLd@?@", leg.legGeometry.getPoints());
    }

    private static TripPlan getTripPlan(GenericLocation from, GenericLocation to, Consumer<RoutingRequest> func) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

        request.modes = new TraverseModeSet(TraverseMode.TRANSIT, TraverseMode.WALK);

        request.setRoutingContext(graph);

        request.maxWalkDistance = 1000;
        request.wheelchairAccessible = true;
        request.setNumItineraries(3);

        func.accept(request);

        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

        return GraphPathToTripPlanConverter.generatePlan(paths, request);
    }

    private static TripPlan getTripPlan(GenericLocation from, GenericLocation to) {
        return getTripPlan(from, to, (r) -> {});
    }

}
