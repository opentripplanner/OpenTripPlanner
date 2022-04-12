package org.opentripplanner.routing.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.routing.algorithm.astar.AStarBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the logic for repeatedly building shortest path trees and accumulating paths
 * through the graph until the requested number of them have been found. It is used in
 * point-to-point (i.e. not one-to-many / analyst) routing.
 * <p>
 * Its exact behavior will depend on whether the routing request allows transit.
 * <p>
 * When using transit it will incorporate techniques from what we called "long distance" mode, which
 * is designed to provide reasonable response times when routing over large graphs (e.g. the entire
 * Netherlands or New York State). In this case it only uses the street network at the first and
 * last legs of the trip, and all other transfers between transit vehicles will occur via
 * PathTransfer edges which are pre-computed by the graph builder.
 * <p>
 * More information is available on the OTP wiki at: https://github.com/openplans/OpenTripPlanner/wiki/LargeGraphs
 * <p>
 * One instance of this class should be constructed per search (i.e. per RoutingRequest: it is
 * request-scoped). Its behavior is undefined if it is reused for more than one search.
 * <p>
 * It is very close to being an abstract library class with only static functions. However it turns
 * out to be convenient and harmless to have the OTPServer object etc. in fields, to avoid passing
 * context around in function parameters.
 */
public class GraphPathFinder {

  private static final Logger LOG = LoggerFactory.getLogger(GraphPathFinder.class);

  Router router;

  public GraphPathFinder(Router router) {
    this.router = router;
  }

  /**
   * This no longer does "trip banning" to find multiple itineraries. It just searches once trying
   * to find a non-transit path.
   */
  public List<GraphPath> getPaths(RoutingContext routingContext) {
    if (routingContext == null) {
      LOG.error("PathService was passed a null routing context.");
      return null;
    }

    RoutingRequest options = routingContext.opt;

    if (options.streetSubRequestModes.isTransit()) {
      throw new UnsupportedOperationException("Transit search not supported");
    }

    AStarBuilder aStar = AStarBuilder
      .oneToOneMaxDuration(Duration.ofSeconds((long) options.maxDirectStreetDurationSeconds))
      // FORCING the dominance function to weight only
      .setDominanceFunction(new DominanceFunction.MinimumWeight())
      .setContext(routingContext)
      .setTimeout(Duration.ofMillis((long) (router.streetRoutingTimeoutSeconds() * 1000)));

    // If this Router has a GraphVisualizer attached to it, set it as a callback for the AStar search
    if (router.graphVisualizer != null) {
      aStar.setTraverseVisitor(router.graphVisualizer.traverseVisitor);
    }

    LOG.debug("rreq={}", options);

    long searchBeginTime = System.currentTimeMillis();
    LOG.debug("BEGIN SEARCH");

    List<GraphPath> paths = aStar.getPathsToTarget();

    LOG.debug("we have {} paths", paths.size());
    LOG.debug("END SEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
    paths.sort(options.getPathComparator(options.arriveBy));
    return paths;
  }

  /**
   * Try to find N paths through the Graph
   */
  public List<GraphPath> graphPathFinderEntryPoint(RoutingContext routingContext) {
    RoutingRequest request = routingContext.opt;
    Instant reqTime = request.getDateTime().truncatedTo(ChronoUnit.MILLIS);

    // We used to perform a protective clone of the RoutingRequest here.
    // There is no reason to do this if we don't modify the request.
    // Any code that changes them should be performing the copy!

    List<GraphPath> paths;
    try {
      paths = getPaths(routingContext);
      if (paths == null && request.wheelchairAccessible) {
        // There are no paths that meet the user's slope restrictions.
        // Try again without slope restrictions, and warn the user in the response.
        RoutingRequest relaxedRequest = request.clone();
        relaxedRequest.maxWheelchairSlope = Double.MAX_VALUE;
        routingContext.slopeRestrictionRemoved = true;
        var relaxedContext = new RoutingContext(
          relaxedRequest,
          routingContext.graph,
          routingContext.fromVertices,
          routingContext.toVertices
        );
        paths = getPaths(relaxedContext);
      }
    } catch (RoutingValidationException e) {
      if (e.getRoutingErrors().get(0).code.equals(RoutingErrorCode.LOCATION_NOT_FOUND)) LOG.info(
        "Vertex not found: " + request.from + " : " + request.to
      );
      throw e;
    }

    // Detect and report that most obnoxious of bugs: path reversal asymmetry.
    // Removing paths might result in an empty list, so do this check before the empty list check.
    if (paths != null) {
      Iterator<GraphPath> gpi = paths.iterator();
      while (gpi.hasNext()) {
        GraphPath graphPath = gpi.next();
        // TODO check, is it possible that arriveBy and time are modifed in-place by the search?
        if (request.arriveBy) {
          if (graphPath.states.getLast().getTime().isAfter(reqTime)) {
            LOG.error("A graph path arrives after the requested time. This implies a bug.");
            gpi.remove();
          }
        } else {
          if (graphPath.states.getFirst().getTime().isBefore(reqTime)) {
            LOG.error("A graph path leaves before the requested time. This implies a bug.");
            gpi.remove();
          }
        }
      }
    }

    if (paths == null || paths.size() == 0) {
      LOG.debug("Path not found: " + request.from + " : " + request.to);
      throw new PathNotFoundException();
    }

    return paths;
  }
}
