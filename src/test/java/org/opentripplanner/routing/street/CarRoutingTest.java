package org.opentripplanner.routing.street;

import static org.opentripplanner.PolylineAssert.assertThatPolylinesAreEqual;

import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
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

public class CarRoutingTest {

    static long dateTime = Instant.now().toEpochMilli();

    /**
     * The OTP algorithm tries hard to never visit the same node twice. This is generally a good
     * idea because it avoids useless loops in the traversal leading to way faster processing time.
     * <p>
     * However there is are certain rare pathological cases where through a series of turn
     * restrictions and roadworks you absolutely must visit a vertex twice if you want to produce a
     * result. One example would be a route like this: https://tinyurl.com/ycqux93g (Note: At the
     * time of writing this Hindenburgstr. (https://www.openstreetmap.org/way/415545869) is closed
     * due to roadworks.)
     * <p>
     * This test checks that such a loop is possible.
     * <p>
     * More information: https://github.com/opentripplanner/OpenTripPlanner/issues/3393
     */
    @Test
    @DisplayName("car routes can contain loops (traversing the same edge twice)")
    public void shouldAllowLoopCausedByTurnRestrictions() {
        var hindenburgStrUnderConstruction = ConstantsForTests.buildOsmGraph(
                ConstantsForTests.HERRENBERG_HINDENBURG_STR_UNDER_CONSTRUCTION_OSM
        );

        var gueltsteinerStr = new GenericLocation(48.59240, 8.87024);
        var aufDemGraben = new GenericLocation(48.59487, 8.87133);

        var polyline =
                computePolyline(hindenburgStrUnderConstruction, gueltsteinerStr, aufDemGraben);

        assertThatPolylinesAreEqual(
                polyline,
                "ouqgH}mcu@gAE]U}BaA]Q}@]uAs@[SAm@Ee@AUEi@XEQkBQ?Bz@Dt@Dh@@TGBC@KBSHGx@"
        );
    }

    private static String computePolyline(Graph graph, GenericLocation from, GenericLocation to) {
        RoutingRequest request = new RoutingRequest();
        request.dateTime = dateTime;
        request.from = from;
        request.to = to;

        request.streetSubRequestModes = new TraverseModeSet(TraverseMode.CAR);

        request.setRoutingContext(graph);

        var gpf = new GraphPathFinder(new Router(graph, RouterConfig.DEFAULT));
        var paths = gpf.graphPathFinderEntryPoint(request);

        var itineraries = GraphPathToItineraryMapper.mapItineraries(paths, request);
        // make sure that we only get CAR legs
        itineraries.forEach(
                i -> i.legs.forEach(l -> Assertions.assertEquals(l.mode, TraverseMode.CAR))
        );
        return itineraries.get(0).legs.get(0).legGeometry.getPoints();
    }
}
