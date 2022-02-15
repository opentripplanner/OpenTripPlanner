package org.opentripplanner.routing.algorithm.astar.strategies;

import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

public class VehicleToStopSkipEdgeStrategy implements SkipEdgeStrategy {

    private final double durationInSeconds;
    private final Function<Stop, Set<Route>> getRoutesForStop;

    private double sumOfScores;
    private final int maxScore;

    public VehicleToStopSkipEdgeStrategy(
            double durationInSeconds,
            Function<Stop, Set<Route>> getRoutesForStop
    ) {
        this.durationInSeconds = durationInSeconds;
        this.maxScore = 3000;
        this.getRoutesForStop = getRoutesForStop;
    }

    @Override
    public boolean shouldSkipEdge(
            Set<Vertex> origins,
            Set<Vertex> targets,
            State current,
            Edge edge,
            ShortestPathTree spt,
            RoutingRequest traverseOptions
    ) {
        if (current.getVertex() instanceof TransitStopVertex) {
            var stopVertex = (TransitStopVertex) current.getVertex();
            var stop = stopVertex.getStop();
            var score = getRoutesForStop.apply(stop)
                    .stream()
                    .map(Route::getMode)
                    .mapToInt(VehicleToStopSkipEdgeStrategy::score)
                    .sum();

            sumOfScores = sumOfScores + score;
        }
        return sumOfScores >= maxScore || current.getElapsedTimeSeconds() > durationInSeconds;
    }

    private static int score(TransitMode mode) {
        switch (mode) {
            case RAIL:
            case FERRY:
            case SUBWAY:
                return 10;
            case BUS:
                return 1;
            default:
                return 2;
        }
    }
}
