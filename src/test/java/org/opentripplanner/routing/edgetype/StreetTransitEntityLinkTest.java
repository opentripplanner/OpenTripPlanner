package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.model.AccessibilityRequirements.EvaluationType.ALLOW_UNKNOWN_INFORMATION;
import static org.opentripplanner.model.AccessibilityRequirements.EvaluationType.KNOWN_INFORMATION_ONLY;
import static org.opentripplanner.model.WheelChairBoarding.NOT_POSSIBLE;
import static org.opentripplanner.model.WheelChairBoarding.NO_INFORMATION;
import static org.opentripplanner.model.WheelChairBoarding.POSSIBLE;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.AccessibilityRequirements;
import org.opentripplanner.model.AccessibilityRequirements.EvaluationType;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.SimpleVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

class StreetTransitEntityLinkTest {

    Graph graph = new Graph();

    Stop inaccessibleStop =
            Stop.stopForTest("A:inaccessible", "wheelchair inaccessible stop", 10.001,
                    10.001, null, NOT_POSSIBLE
            );
    Stop accessibleStop =
            Stop.stopForTest("A:accessible", "wheelchair accessible stop", 10.001,
                    10.001, null, POSSIBLE
            );

    Stop unknownStop =
            Stop.stopForTest("A:unknown", "unknown", 10.001,
                    10.001, null, NO_INFORMATION
            );

    @Test
    void disallowInaccessibleStop() {
        var afterTraversal = traverse(inaccessibleStop, KNOWN_INFORMATION_ONLY);
        assertNull(afterTraversal);
    }

    @Test
    void allowAccessibleStop() {
        State afterTraversal = traverse(accessibleStop, KNOWN_INFORMATION_ONLY);

        assertNotNull(afterTraversal);
    }

    @Test
    void unknownStop() {
        var afterTraversal = traverse(unknownStop, ALLOW_UNKNOWN_INFORMATION);
        assertNotNull(afterTraversal);

        var afterStrictTraversal = traverse(unknownStop, KNOWN_INFORMATION_ONLY);
        assertNull(afterStrictTraversal);
    }

    private State traverse(Stop stop, EvaluationType evaluation) {
        var from = new SimpleVertex(graph, "A", 10, 10);
        var to = new TransitStopVertex(graph, stop, Set.of(TransitMode.RAIL));

        var req = new RoutingRequest();
        req.accessibilityRequirements = AccessibilityRequirements.makeDefault(evaluation);

        var ctx = new RoutingContext(req, graph, from, to);
        var state = new State(ctx);

        var edge = new StreetTransitStopLink(from, to);

        return edge.traverse(state);
    }
}