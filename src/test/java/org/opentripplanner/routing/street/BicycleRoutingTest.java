package org.opentripplanner.routing.street;

import static org.opentripplanner.PolylineAssert.assertThatPolylinesAreEqual;

import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;


public class BicycleRoutingTest {

    static long dateTime = Instant.now().toEpochMilli();
    Graph herrenbergGraph = ConstantsForTests.buildOsmGraph(ConstantsForTests.HERRENBERG_OSM);

    /**
     * https://www.openstreetmap.org/way/22392895 is access=destination which means that both
     * bicycles and motor vehicles must not pass through.
     */
    @Test
    public void shouldRespectGeneralNoThroughTraffic() {
        var mozartStr = new GenericLocation(48.59713, 8.86107);
        var fritzLeharStr = new GenericLocation(48.59696, 8.85806);

        var polyline1 = computePolyline(herrenbergGraph, mozartStr, fritzLeharStr);
        assertThatPolylinesAreEqual(polyline1, "_srgHutau@h@B|@Jf@BdAG?\\JT@jA?DSp@_@fFsAT{@DBpC");

        var polyline2 = computePolyline(herrenbergGraph, fritzLeharStr, mozartStr);
        assertThatPolylinesAreEqual(polyline2, "{qrgH{aau@CqCz@ErAU^gFRq@?EAkAKU?]eAFg@C}@Ki@C");
    }

    /**
     * Tests that that https://www.openstreetmap.org/way/35097400 is allowed for cars due to
     * motor_vehicle=destination being meant for cars only.
     */
    @Test
    public void shouldNotRespectMotorCarNoThru() {
        var schiessmauer = new GenericLocation(48.59737, 8.86350);
        var zeppelinStr = new GenericLocation(48.59972, 8.86239);

        var polyline1 = computePolyline(herrenbergGraph, schiessmauer, zeppelinStr);
        assertThatPolylinesAreEqual(polyline1, "otrgH{cbu@S_AU_AmAdAyApAGDs@h@_@\\_ClBe@^?S");

        var polyline2 = computePolyline(herrenbergGraph, zeppelinStr, schiessmauer);
        assertThatPolylinesAreEqual(polyline2, "ccsgH{|au@?Rd@_@~BmB^]r@i@FExAqAlAeAT~@R~@");
    }

    private static String computePolyline(Graph graph, GenericLocation from, GenericLocation to) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

        request.streetSubRequestModes = new TraverseModeSet(TraverseMode.BICYCLE);
        request.setRoutingContext(graph);

        var gpf = new GraphPathFinder(new Router(graph, RouterConfig.DEFAULT));
        var paths = gpf.graphPathFinderEntryPoint(request);

        var itineraries = GraphPathToItineraryMapper.mapItineraries(paths, request);
        // make sure that we only get BICYLE legs
        itineraries.forEach(i -> i.legs.forEach(l -> Assertions.assertEquals(l.mode, TraverseMode.BICYCLE)));
        return itineraries.get(0).legs.get(0).legGeometry.getPoints();
    }
}
