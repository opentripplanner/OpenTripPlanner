package org.opentripplanner.routing.core;


import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    static long dateTime = OffsetDateTime.parse("2021-09-16T10:15:29-04:00").toEpochSecond();

    static Graph graph = getDefaultGraph();

    public static Graph getDefaultGraph() {
        return ConstantsForTests.getGraph(ConstantsForTests.CENTRAL_ATLANTA_OSM, ConstantsForTests.ATLANTA_BLUE_GREEN_LINE);
    }

    @Test
    public void shouldPopulateAccessibilityScoreOnlyForAccessibleRoutes() {
        GenericLocation start = new GenericLocation(33.74703, -84.39440);
        GenericLocation end = new GenericLocation(33.75029, -84.39188);

        // wheelchair=true is set by default, check method
        Itinerary i = getTripPlan(start, end, r -> r.setMode(TraverseMode.WALK)).itinerary.get(0);
        List<Leg> legs = i.legs;

        assertEquals(1, legs.size());

        Leg leg = legs.get(0);
        assertNotNull(leg.accessibilityScore);

        i = getTripPlan(start, end, r -> {
            r.setMode(TraverseMode.WALK);
            r.wheelchairAccessible = false;
        }).itinerary.get(0);
        leg = i.legs.get(0);
        assertNull(leg.accessibilityScore);
    }

    @Test
    public void canScoreAccessibleTrip() {
        // near Georgia State station
        GenericLocation start = new GenericLocation(33.75054, -84.38409);
        // near Ashby station
        GenericLocation end = new GenericLocation(33.75744, -84.41754);

        Itinerary i = getTripPlan(start, end).itinerary.get(0);
        List<Leg> transitLegs = i.legs.stream().filter(Leg::isTransitLeg).collect(Collectors.toList());

        assertEquals(1, transitLegs.size());

        Leg leg = transitLegs.get(0);
        assertBlueLineFromGeorgiaStateToAshby(leg);
    }

    @Test
    public void canScoreTripWhereStartIsNotAccessible() {
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
        assertBlueLineFromGeorgiaStateToAshby(leg);
    }

    private void assertBlueLineFromGeorgiaStateToAshby(Leg leg) {
        assertEquals("BLUE", leg.routeShortName);

        assertEquals("GEORGIA STATE STATION", leg.from.name);
        assertEquals("ASHBY STATION", leg.to.name);
        // since both the start and end stops and the trip are accessible, we get a perfect score
        assertEquals(1, leg.accessibilityScore);
    }

    @Test
    public void canScoreTripWhereStartHasUnknownAccessibility() {
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
    public void canScoreTripWithInaccessibleTransitLeg() {
        // near Georgia State station
        GenericLocation start = new GenericLocation(33.75054, -84.38409);
        // near Ashby station
        GenericLocation end = new GenericLocation(33.75744, -84.41754);

        // we are banning the blue line even though it is accessible to force getting on the green one
        Itinerary i = getTripPlan(start, end, r -> r.setBannedRoutes("MARTA_BLUE")).itinerary.get(0);
        List<Leg> transitLegs = i.legs.stream().filter(Leg::isTransitLeg).collect(Collectors.toList());

        Leg leg = transitLegs.get(0);
        assertEquals("GREEN", leg.routeShortName);

        assertEquals("GEORGIA STATE STATION", leg.from.name);
        assertEquals("ASHBY STATION", leg.to.name);

        // because are using a trip that has unknown accessibility, it has a lower score
        assertEquals(0.8333f, leg.accessibilityScore, DELTA);
    }

    @Test
    public void canApplyStairsPenalty() {
        GenericLocation start = new GenericLocation(33.75630, -84.39527);
        GenericLocation end = new GenericLocation(33.75649, -84.39580);

        Itinerary i = getTripPlan(start, end, r -> {
            r.wheelchairStairsReluctance = 50;
            r.setMode(TraverseMode.WALK);
        }).itinerary.get(0);
        Leg leg = i.legs.get(0);
        assertEquals("WALK", leg.mode);
        assertThatPolylinesAreEqual("y_`mEnmbbObA}@U]{@wAIMEOMHwBz@^p@Zn@Zx@Ld@DJLd@?@", leg.legGeometry.getPoints());

        // if we reduce the stair penalty for wheelchairs we get a route that goes up the stairs
        i = getTripPlan(start, end, r -> {
            r.setMode(TraverseMode.WALK);
            r.wheelchairStairsReluctance = 0;
        }).itinerary.get(0);

        leg = i.legs.get(0);
        assertEquals("WALK", leg.mode);
        assertThatPolylinesAreEqual("y_`mEnmbbO[XIHBFUTLd@?@", leg.legGeometry.getPoints());
    }

    @Test
    public void canApplyNoWheelchairAccessStreetPenalty() {
        // since i cannot find a real-life example of an inaccessible street in central Atlanta
        // i manually set Hogue Street Northeast (https://www.openstreetmap.org/way/9254841)
        // to be inaccessible in the OSM data

        GenericLocation start = new GenericLocation(33.75545, -84.37105);
        GenericLocation end = new GenericLocation(33.75767, -84.37120);

        Itinerary i = getTripPlan(start, end, r -> r.setMode(TraverseMode.WALK)).itinerary.get(0);
        Leg leg = i.legs.get(0);
        assertEquals("WALK", leg.mode);
        assertThatPolylinesAreEqual("sz_mE`v}aO?@?L?|AAzAM?A?G@SA[?I?E?C?E?oDCG?E?A?U?[?I??_@iAA@cAM?@o@?O", leg.legGeometry.getPoints());

        // if we reduce the reduce the reluctance for wheelchair-inaccessible streets we get a route that uses
        // Hogue Street Northeast
        i = getTripPlan(start, end, r -> {
            r.setMode(TraverseMode.WALK);
            r.noWheelchairAccessOnStreetReluctance = 0;
        }).itinerary.get(0);

        leg = i.legs.get(0);
        assertEquals("WALK", leg.mode);
        assertEquals(0.5f, leg.accessibilityScore);
        assertThatPolylinesAreEqual("sz_mE`v}aO?@M?cA?}A?uFCM??L?R", leg.legGeometry.getPoints());
    }

    @Test
    public void canScoreAccessibleTransfers() {
        // Memorial Drive
        GenericLocation start = new GenericLocation(33.74684, -84.37910);

        // near Ashby station
        GenericLocation end = new GenericLocation(33.75744, -84.41754);

        Itinerary i = getTripPlan(start, end).itinerary.get(0);
        Leg leg = i.legs.get(1);

        // take bus 21 to near Georgia State station
        // line 21 is trimmed to force non-wheelchair users to transfer at Georgia State as it would be
        // quicker to stay on the bus and get on the metro at Five Points
        assertEquals("MEMORIAL DR SE @ HILL ST SE", leg.from.name);
        assertEquals("MEMORIAL DR SE @ FRASER ST SE", leg.to.name);
        assertEquals("BUS", leg.mode);
        assertEquals("21", leg.routeShortName);

        // we get a path full of detours because we have made Capitol Avenue Southeast
        // (https://www.openstreetmap.org/way/849819590) wheelchair-inaccessible (manually in the osm.pbf file) and have to go around it
        leg = i.legs.get(2);
        assertEquals("WALK", leg.mode);
        assertThatPolylinesAreEqual(
                "ge~lEtu`bO@vA?DULA?AJ?rA?b@C?cBCGDk@?ECCCODSFI??KC?CACCMFS@Q?QEQIKCGEMEUMKIGPKEKEICGIOKOIIBGBGJc@U{@g@??w@o@]]SSz@uA",
                leg.legGeometry.getPoints()
        );

        // we get on the blue line at Georgia State and ride to Ashby
        leg = i.legs.get(3);
        assertEquals("GEORGIA STATE STATION", leg.from.name);
        assertEquals("ASHBY STATION", leg.to.name);
        assertEquals("BLUE", leg.routeShortName);

        // we plan the same trip again but this time we are not in a wheelchair
        i = getTripPlan(start, end, r -> r.wheelchairAccessible = false).itinerary.get(0);
        leg = i.legs.get(1);

        // same as above, take the bus to near Georgia state
        assertEquals("21", leg.routeShortName);
        assertEquals("MEMORIAL DR SE @ HILL ST SE", leg.from.name);

        leg = i.legs.get(2);
        assertEquals("WALK", leg.mode);
        // here we are not in a wheelchair and hence can use the street segments that are inaccessible
        // for a wheelchair
        assertThatPolylinesAreEqual(
                "ge~lEtu`bO@vA?D?D@pC?XY?q@@_CAOCWEO?[@_@CSGQEGA{@Yo@UQGWMc@U{@g@??w@o@]]SSz@uA",
                leg.legGeometry.getPoints()
        );

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
