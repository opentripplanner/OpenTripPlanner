package org.opentripplanner.ext.carpooling.street;

import java.time.Duration;
import org.opentripplanner.astar.strategy.ComposingSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the path of a corridor segment: an A* car search between its two vertices, bounded by the
 * maximum trip duration and never crossing another request's temporary edges.
 * <p>
 * A failed search is logged and answered with {@code null}; a cancelled request propagates, see
 * {@link CarpoolRouter#route}.
 */
public class CarpoolStreetRouter implements CarpoolRouter {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolStreetRouter.class);

  private final StreetLimitationParametersService streetLimitationParametersService;
  private final Duration maxTripDuration;

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

  /** The best path, or {@code null} if none reaches the destination within the bound. */
  private StreetPath carpoolRouting(Vertex fromVertex, Vertex toVertex) {
    var request = StreetSearchRequest.of().withMode(StreetMode.CAR).build();
    var streetSearch = StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withHeuristic(new EuclideanRemainingWeightHeuristic(streetLimitationParametersService))
      .withSkipEdgeStrategy(
        new ComposingSkipEdgeStrategy<>(
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

    return paths.getFirst();
  }
}
