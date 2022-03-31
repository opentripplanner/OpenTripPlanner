package org.opentripplanner.ext.vehicletostopheuristics;

import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_PICKUP;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.SCOOTER_RENTAL;

import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.astar.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
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

    private final Function<Stop, Set<Route>> getRoutesForStop;

    private double sumOfScores;
    private final int maxScore;

    public static final Set<StreetMode> applicableModes =
            Set.of(
                    BIKE_TO_PARK,
                    BIKE_RENTAL,
                    CAR_TO_PARK,
                    CAR_PICKUP,
                    CAR_RENTAL,
                    SCOOTER_RENTAL
            );


    public VehicleToStopSkipEdgeStrategy(Function<Stop, Set<Route>> getRoutesForStop) {
        this.maxScore = 300;
        this.getRoutesForStop = getRoutesForStop;
    }

    @Override
    public boolean shouldSkipEdge(State current, Edge edge) {
        if (current.getNonTransitMode().isWalking()) {
            if (current.getVertex() instanceof TransitStopVertex stopVertex) {
                var score = getRoutesForStop.apply(stopVertex.getStop())
                        .stream()
                        .map(Route::getMode)
                        .mapToInt(VehicleToStopSkipEdgeStrategy::score)
                        .sum();

                sumOfScores = sumOfScores + score;
            }
            return false;
        }
        else {
            return sumOfScores >= maxScore;
        }
    }

    private static int score(TransitMode mode) {
        return switch (mode) {
            case RAIL, FERRY, SUBWAY -> 20;
            case BUS -> 1;
            default -> 2;
        };
    }
}
