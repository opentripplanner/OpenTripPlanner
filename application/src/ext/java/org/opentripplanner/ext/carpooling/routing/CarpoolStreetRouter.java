package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import org.opentripplanner.astar.strategy.ComposingSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.ext.carpooling.model.GraphPath;
import org.opentripplanner.ext.carpooling.util.TraversalScope;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.utils.collection.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Street routing service for carpooling insertion evaluation.
 * <p>
 * This router encapsulates all dependencies needed for A* street routing between two street
 * vertices during carpool insertion optimization. It handles search configuration and path
 * selection for CAR mode routing.
 *
 * <h2>Routing Strategy</h2>
 * <ul>
 *   <li><strong>Mode:</strong> CAR mode for both origin and destination</li>
 *   <li><strong>Algorithm:</strong> A* with Euclidean heuristic</li>
 *   <li><strong>Dominance:</strong> Minimum weight</li>
 *   <li><strong>Search Bound:</strong> the maximum trip duration</li>
 *   <li><strong>Error Handling:</strong> Returns null on routing failure (logged as warning); a
 *       cancelled request propagates, see {@link CarpoolRouter#route}</li>
 * </ul>
 *
 * @see InsertionEvaluator for usage in insertion evaluation
 */
public class CarpoolStreetRouter implements CarpoolRouter {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolStreetRouter.class);

  private final StreetLimitationParametersService streetLimitationParametersService;
  private final Duration maxTripDuration;

  /**
   * Creates a new carpool street router.
   *
   * @param streetLimitationParametersService provides street routing parameters (speed limits, etc.)
   * @param maxTripDuration the bound of every search, see
   *        {@link org.opentripplanner.ext.carpooling.CarpoolingParameters#maxTripDuration()}
   */
  public CarpoolStreetRouter(
    StreetLimitationParametersService streetLimitationParametersService,
    Duration maxTripDuration
  ) {
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.maxTripDuration = maxTripDuration;
  }

  @Override
  public RoutedSegment route(Vertex from, Vertex to) {
    try {
      var path = carpoolRouting(from, to);
      return path == null ? null : RoutedSegment.of(path);
    } catch (OTPRequestTimeoutException e) {
      // Rethrown ahead of the catch-all below, which would turn a cancellation into a null return.
      throw e;
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
   *   <li><strong>Skip Strategy:</strong> Duration-based edge skipping, and no other request's
   *       temporary edges</li>
   *   <li><strong>Dominance:</strong> Minimum weight</li>
   * </ul>
   *
   * @param fromVertex the origin vertex
   * @param toVertex the destination vertex
   * @return the first (best) path found, or null if no path reaches the destination within
   *         the maximum trip duration
   */
  private GraphPath<State, Edge, Vertex> carpoolRouting(Vertex fromVertex, Vertex toVertex) {
    var request = StreetSearchRequest.of().withMode(StreetMode.CAR).build();
    var streetSearch = StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withHeuristic(new EuclideanRemainingWeightHeuristic(streetLimitationParametersService))
      // Bound the search at the carpool trip ceiling rather than the passenger request's
      // maxDirectDuration: a driver leg is not a passenger direct trip, and a request-independent
      // bound keeps the cached baseline leg durations request-independent too.
      .withSkipEdgeStrategy(
        new ComposingSkipEdgeStrategy<>(
          // Never another request's temporary edges: a baseline leg is kept for the trip's lifetime.
          TraversalScope.withOwnLinkingOf(fromVertex, toVertex),
          new DurationSkipEdgeStrategy<>(maxTripDuration)
        )
      )
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withRequest(request)
      .withFrom(fromVertex)
      .withTo(toVertex);

    var paths = streetSearch.getPathsToTarget();

    if (paths.isEmpty()) {
      return null;
    }

    var streetPath = paths.getFirst();
    var edges = ListUtils.ofIterable(streetPath.lastState().listBackEdges());
    return new GraphPath<>(streetPath.states(), edges);
  }
}
