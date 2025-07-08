package org.opentripplanner.routing.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.PathComparator;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.street.model.StreetConstants;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
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
 * One instance of this class should be constructed per search (i.e. per RouteRequest: it is
 * request-scoped). Its behavior is undefined if it is reused for more than one search.
 * <p>
 * It is very close to being an abstract library class with only static functions. However it turns
 * out to be convenient and harmless to have the OTPServer object etc. in fields, to avoid passing
 * context around in function parameters.
 */
public class GraphPathFinder {

  private static final Logger LOG = LoggerFactory.getLogger(GraphPathFinder.class);

  @Nullable
  private final TraverseVisitor<State, Edge> traverseVisitor;

  private final DataOverlayContext dataOverlayContext;

  private final float maxCarSpeed;

  public GraphPathFinder(@Nullable TraverseVisitor<State, Edge> traverseVisitor) {
    this(traverseVisitor, null, StreetConstants.DEFAULT_MAX_CAR_SPEED);
  }

  public GraphPathFinder(
    @Nullable TraverseVisitor<State, Edge> traverseVisitor,
    @Nullable DataOverlayContext dataOverlayContext,
    float maxCarSpeed
  ) {
    this.traverseVisitor = traverseVisitor;
    this.dataOverlayContext = dataOverlayContext;
    this.maxCarSpeed = maxCarSpeed;
  }

  /**
   * This no longer does "trip banning" to find multiple itineraries. It just searches once trying
   * to find a non-transit path.
   */
  public List<GraphPath<State, Edge, Vertex>> getPaths(
    RouteRequest request,
    Set<Vertex> from,
    Set<Vertex> to
  ) {
    StreetPreferences preferences = request.preferences().street();

    StreetSearchBuilder aStar = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic(maxCarSpeed))
      .setSkipEdgeStrategy(
        new DurationSkipEdgeStrategy(
          preferences.maxDirectDuration().valueOf(request.journey().direct().mode())
        )
      )
      // FORCING the dominance function to weight only
      .setDominanceFunction(new DominanceFunctions.MinimumWeight())
      .setRequest(request)
      .setStreetRequest(request.journey().direct())
      .setFrom(from)
      .setTo(to)
      .setDataOverlayContext(dataOverlayContext);

    // If the search has a traverseVisitor(GraphVisualizer) attached to it, set it as a callback
    // for the AStar search
    if (traverseVisitor != null) {
      aStar.setTraverseVisitor(traverseVisitor);
    }

    LOG.debug("rreq={}", request);

    long searchBeginTime = System.currentTimeMillis();
    LOG.debug("BEGIN SEARCH");

    List<GraphPath<State, Edge, Vertex>> paths = aStar.getPathsToTarget();

    LOG.debug("we have {} paths", paths.size());
    LOG.debug("END SEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
    paths.sort(new PathComparator(request.arriveBy()));
    return paths;
  }

  /**
   * Try to find N paths through the Graph
   */
  public List<GraphPath<State, Edge, Vertex>> graphPathFinderEntryPoint(
    RouteRequest request,
    TemporaryVerticesContainer vertexContainer
  ) {
    return graphPathFinderEntryPoint(
      request,
      vertexContainer.getFromVertices(),
      vertexContainer.getToVertices()
    );
  }

  public List<GraphPath<State, Edge, Vertex>> graphPathFinderEntryPoint(
    RouteRequest request,
    Set<Vertex> from,
    Set<Vertex> to
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    var reqTime = request.dateTime() == null ? RouteRequest.normalizeNow() : request.dateTime();

    List<GraphPath<State, Edge, Vertex>> paths = getPaths(request, from, to);

    // Detect and report that most obnoxious of bugs: path reversal asymmetry.
    // Removing paths might result in an empty list, so do this check before the empty list check.
    if (paths != null) {
      Iterator<GraphPath<State, Edge, Vertex>> gpi = paths.iterator();
      while (gpi.hasNext()) {
        GraphPath<State, Edge, Vertex> graphPath = gpi.next();
        // TODO check, is it possible that arriveBy and time are modifed in-place by the search?
        if (request.arriveBy()) {
          if (graphPath.states.getLast().getTimeAccurate().isAfter(reqTime)) {
            LOG.error(
              "A graph path arrives {} after the requested time {}. This implies a bug.",
              graphPath.states.getLast().getTimeAccurate(),
              reqTime
            );
            gpi.remove();
          }
        } else {
          if (graphPath.states.getFirst().getTimeAccurate().isBefore(reqTime)) {
            LOG.error(
              "A graph path leaves {} before the requested time {}. This implies a bug.",
              graphPath.states.getFirst().getTimeAccurate(),
              reqTime
            );
            gpi.remove();
          }
        }
      }
    }

    if (paths == null || paths.isEmpty()) {
      LOG.debug("Path not found: {} : {}", request.from(), request.to());
      throw new PathNotFoundException();
    }

    return paths;
  }
}
