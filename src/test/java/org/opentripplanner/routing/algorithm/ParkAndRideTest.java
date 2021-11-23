package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;

public abstract class ParkAndRideTest extends GraphRoutingTest {

    protected Graph graph;

    public static List<String> graphPath(GraphPath graphPath) {
        return graphPath.states.stream()
                .map(s -> String.format("%s%s - %s (%,.2f, %d)",
                        s.getBackMode(),
                        s.isVehicleParked() ? " (parked)" : "",
                        s.getBackEdge() != null ? s.getBackEdge().getName() : null,
                        s.getWeight(),
                        s.getElapsedTimeSeconds()
                ))
                .collect(Collectors.toList());
    }

    protected void assertEmptyPath(
            Vertex fromVertex,
            Vertex toVertex,
            StreetMode streetMode
    ) {
        assertPath(fromVertex, toVertex, streetMode, false, List.of(), List.of());
    }

    protected void assertEmptyPath(
            Vertex fromVertex,
            Vertex toVertex,
            StreetMode streetMode,
            boolean requireWheelChairAccessible
    ) {
        assertPath(fromVertex, toVertex, streetMode, requireWheelChairAccessible, List.of(), List.of());
    }

    protected void assertPath(
            Vertex fromVertex,
            Vertex toVertex,
            StreetMode streetMode,
            String ... descriptor
    ) {
        assertPath(fromVertex, toVertex, streetMode, false, List.of(descriptor), List.of(descriptor));
    }

    protected void assertPath(
            Vertex fromVertex,
            Vertex toVertex,
            StreetMode streetMode,
            boolean requireWheelChairAccessible,
            String ... descriptor
    ) {
        assertPath(fromVertex, toVertex, streetMode, requireWheelChairAccessible, List.of(descriptor), List.of(descriptor));
    }

    private void assertPath(
            Vertex fromVertex,
            Vertex toVertex,
            StreetMode streetMode,
            boolean requireWheelChairAccessible,
            List<String> departAtDescriptor,
            List<String> arriveByDescriptor
    ) {
        List<String> departAt = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, streetMode, requireWheelChairAccessible, false);
        List<String> arriveBy = runStreetSearchAndCreateDescriptor(fromVertex, toVertex, streetMode, requireWheelChairAccessible, true);

        assertEquals(departAtDescriptor, departAt, "departAt path");
        assertEquals(arriveByDescriptor, arriveBy, "arriveBy path");
    }

    protected List<String> runStreetSearchAndCreateDescriptor(
            Vertex fromVertex,
            Vertex toVertex,
            StreetMode streetMode,
            boolean requireWheelChairAccessible,
            boolean arriveBy
    ) {
        var options = new RoutingRequest().getStreetSearchRequest(streetMode);
        options.bikeParkCost = 120;
        options.bikeParkTime = 60;
        options.carParkCost = 240;
        options.carParkTime = 180;
        options.wheelchairAccessible = requireWheelChairAccessible;
        options.arriveBy = arriveBy;
        options.setRoutingContext(graph, fromVertex, toVertex);

        var tree = new AStar().getShortestPathTree(options);
        var path = tree.getPath(
                arriveBy ? fromVertex : toVertex,
                false
        );

        if (path == null) {
            return List.of();
        }

        return path.states
                .stream()
                .map(s -> String.format(
                        Locale.ROOT,
                        "%s%s - %s (%,.2f, %d)",
                        s.getBackMode(),
                        s.isVehicleParked() ? " (parked)" : "",
                        s.getBackEdge() != null ? s.getBackEdge().getName() : null,
                        s.getWeight(),
                        s.getElapsedTimeSeconds()
                ))
                .collect(Collectors.toList());
    }
}
