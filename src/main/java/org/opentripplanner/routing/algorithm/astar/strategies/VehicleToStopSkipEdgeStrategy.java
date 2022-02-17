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

/**
 * This strategy terminates when enough "important" stops are found.
 * <p>
 * The definition of important is a stop where many routes of mode RAIL, SUBWAY or FERRY depart.
 * <p>
 * This means that the search radius scales with density of "important" stops:
 * <p>
 * <li>in a city the radius is quite small
 * <li>in more rural regions the radius is bigger and stops further away are considered
 * <p>
 * The strategy is useful when you want to limit the number of accesses of Park+Ride, Bike+Ride and
 * Bike+Transit: it improves both performance the quality of results.
 * <p>
 * {@see https://github.com/opentripplanner/OpenTripPlanner/pull/3906}
 */
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
                return 20;
            case BUS:
                return 1;
            default:
                return 2;
        }
    }
}
