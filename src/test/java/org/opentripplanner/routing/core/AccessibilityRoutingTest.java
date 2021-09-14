package org.opentripplanner.routing.core;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
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

    static long dateTime = OffsetDateTime.parse("2021-09-14T09:56:29-04:00").toEpochSecond();

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

        Itinerary i = getTripPlan(graph, start, end).itinerary.get(0);
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

        Itinerary i = getTripPlan(graph, start, end).itinerary.get(0);
        List<Leg> transitLegs = i.legs.stream().filter(Leg::isTransitLeg).collect(Collectors.toList());

        assertEquals(1, transitLegs.size());

        Leg leg = transitLegs.get(0);
        assertEquals("BLUE", leg.routeShortName);

        assertEquals("FIVE POINTS STATION", leg.from.name);
        assertEquals("ASHBY STATION", leg.to.name);

        // the start is not accessible, so we get a lowered accessibility score
        assertEquals(0.666666f, leg.accessibilityScore, DELTA);
    }

    private static TripPlan getTripPlan(Graph graph, GenericLocation from, GenericLocation to) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

        request.modes = new TraverseModeSet(TraverseMode.TRANSIT, TraverseMode.WALK);

        request.setRoutingContext(graph);

        request.maxWalkDistance = 1000;
        request.wheelchairAccessible = true;
        request.setNumItineraries(3);

        GraphPathFinder gpf = new GraphPathFinder(new Router(graph.routerId, graph));
        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

        return GraphPathToTripPlanConverter.generatePlan(paths, request);
    }

}
