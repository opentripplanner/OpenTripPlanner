package org.opentripplanner.ext.carpooling.routing;

import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Street routing service for carpooling insertion evaluation.
 * <p>
 * This router encapsulates all dependencies needed for A* street routing between
 * coordinate pairs during carpool insertion optimization. It handles vertex linking,
 * search configuration, and path selection for CAR mode routing.
 *
 * <h2>Routing Strategy</h2>
 * <ul>
 *   <li><strong>Mode:</strong> CAR mode for both origin and destination</li>
 *   <li><strong>Algorithm:</strong> A* with Euclidean heuristic</li>
 *   <li><strong>Dominance:</strong> Minimum weight</li>
 *   <li><strong>Vertex Linking:</strong> Creates temporary vertices at coordinate locations</li>
 *   <li><strong>Error Handling:</strong> Returns null on routing failure (logged as warning)</li>
 * </ul>
 *
 * @see InsertionEvaluator for usage in insertion evaluation
 */
public class CarpoolStreetRouter implements CarpoolRouter {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolStreetRouter.class);

  private final StreetLimitationParametersService streetLimitationParametersService;

  /**
   * Creates a new carpool street router.
   *
   * @param streetLimitationParametersService provides street routing parameters (speed limits, etc.)
   */
  public CarpoolStreetRouter(StreetLimitationParametersService streetLimitationParametersService) {
    this.streetLimitationParametersService = streetLimitationParametersService;
  }

  @Override
  public GraphPath<State, Edge, Vertex> route(Vertex from, Vertex to) {
    try {
      return carpoolRouting(from, to);
    } catch (Exception e) {
      LOG.warn("Routing failed from {} to {}: {}", from, to, e.getMessage());
      return null;
    }
  }

  /**
   * Core A* routing for carpooling optimized for car travel.
   * <p>
   * Configures and executes an A* street search with settings optimized for carpooling:
   * <ul>
   *   <li><strong>Heuristic:</strong> Euclidean distance with max car speed</li>
   *   <li><strong>Skip Strategy:</strong> Duration-based edge skipping</li>
   *   <li><strong>Dominance:</strong> Minimum weight</li>
   * </ul>
   *
   * @param fromVertex the origin vertex
   * @param toVertex the destination vertex
   * @return the first (best) path found, or null if no paths exist
   */
  private GraphPath<State, Edge, Vertex> carpoolRouting(Vertex fromVertex, Vertex toVertex) {
    var request = StreetSearchRequest.of().withMode(StreetMode.CAR).build();
    var streetSearch = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic(streetLimitationParametersService))
      // Bound the search at the carpool trip ceiling rather than the passenger request's
      // maxDirectDuration: a driver leg is not a passenger direct trip, and a request-independent
      // bound keeps the cached baseline leg durations request-independent too.
      .withSkipEdgeStrategy(new DurationSkipEdgeStrategy(CarpoolTrip.MAX_TRIP_DURATION))
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withRequest(request)
      .withFrom(fromVertex)
      .withTo(toVertex);

    var paths = streetSearch.getPathsToTarget();

    if (paths.isEmpty()) {
      return null;
    }

    return paths.getFirst().toGraphPath();
  }
}
