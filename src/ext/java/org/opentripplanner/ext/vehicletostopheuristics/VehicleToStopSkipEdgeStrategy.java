package org.opentripplanner.ext.vehicletostopheuristics;

import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_HAILING;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_PICKUP;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.SCOOTER_RENTAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;

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
public class VehicleToStopSkipEdgeStrategy implements SkipEdgeStrategy<State, Edge> {

  public static final Set<StreetMode> applicableModes = Set.of(
    BIKE_TO_PARK,
    BIKE_RENTAL,
    CAR_TO_PARK,
    CAR_PICKUP,
    CAR_HAILING,
    CAR_RENTAL,
    SCOOTER_RENTAL
  );
  private final Function<RegularStop, Collection<TripPattern>> getPatternsForStop;
  private final int maxScore;
  private final List<TransitFilter> filters;
  private double sumOfScores;

  private final Set<FeedScopedId> stopsCounted = new HashSet<>();

  public VehicleToStopSkipEdgeStrategy(
    Function<RegularStop, Collection<TripPattern>> getPatternsForStop,
    Collection<TransitFilter> filters
  ) {
    this.filters = new ArrayList<>(filters);
    this.maxScore = 300;
    this.getPatternsForStop = getPatternsForStop;
  }

  @Override
  public boolean shouldSkipEdge(State current, Edge edge) {
    if (current.getNonTransitMode().isWalking()) {
      if (
        current.getVertex() instanceof TransitStopVertex stopVertex &&
        !stopsCounted.contains(stopVertex.getStop().getId())
      ) {
        // TODO: 2022-12-05 filters: check performance on that and verify that this is right. Previously we were filtering just on modes
        var stop = stopVertex.getStop();

        // Not using streams. Performance is important here
        var patterns = getPatternsForStop.apply(stop);
        var score = 0;
        for (var pattern : patterns) {
          for (var filter : filters) {
            if (filter.matchTripPattern(pattern)) {
              score += VehicleToStopSkipEdgeStrategy.score(pattern.getMode());
              break;
            }
          }
        }

        stopsCounted.add(stop.getId());

        sumOfScores = sumOfScores + score;
      }
      return false;
    } else {
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
