package org.opentripplanner.ext.carpooling.routing;

import java.util.List;
import java.util.Set;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.PathComparator;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.routing.linking.TemporaryVerticesContainer;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.streetadapter.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.streetadapter.StreetSearchBuilder;
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
public class CarpoolStreetRouter {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolStreetRouter.class);

  private final StreetLimitationParametersService streetLimitationParametersService;
  private final RouteRequest request;
  private final VertexLinker vertexLinker;
  private final TemporaryVerticesContainer temporaryVerticesContainer;

  /**
   * Creates a new carpool street router.
   *
   * @param streetLimitationParametersService provides street routing parameters (speed limits, etc.)
   * @param request the route request containing preferences and timing
   * @param vertexLinker links coordinates to graph vertices
   * @param temporaryVerticesContainer container for temporary vertices and edges
   */
  public CarpoolStreetRouter(
    StreetLimitationParametersService streetLimitationParametersService,
    RouteRequest request,
    VertexLinker vertexLinker,
    TemporaryVerticesContainer temporaryVerticesContainer
  ) {
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.request = request;
    this.vertexLinker = vertexLinker;
    this.temporaryVerticesContainer = temporaryVerticesContainer;
  }

  /**
   * Routes from one location to another using A* street search.
   * <p>
   * Uses the provided linking context to find vertices at the given coordinates,
   * performs A* search, and returns the best path found. Returns null if routing fails.
   *
   * @param from origin coordinate
   * @param to destination coordinate
   * @param linkingContext linking context containing pre-linked vertices
   * @return the best path found, or null if routing failed
   */
  public GraphPath<State, Edge, Vertex> route(
    GenericLocation from,
    GenericLocation to,
    LinkingContext linkingContext
  ) {
    try {
      var fromVertices = getOrCreateVertices(from, linkingContext);
      var toVertices = getOrCreateVertices(to, linkingContext);

      return carpoolRouting(
        new StreetRequest(StreetMode.CAR),
        fromVertices,
        toVertices,
        streetLimitationParametersService.maxCarSpeed()
      );
    } catch (Exception e) {
      LOG.warn("Routing failed from {} to {}: {}", from, to, e.getMessage());
      return null;
    }
  }

  /**
   * Gets vertices for a location, either from the LinkingContext or by creating
   * temporary vertices on-demand.
   * <p>
   * This method first checks if vertices already exist in the LinkingContext (which
   * contains pre-linked vertices for the passenger's origin and destination). If not
   * found (e.g., for driver trip waypoints), it creates a temporary vertex on-demand
   * using VertexLinker and adds it to the TemporaryVerticesContainer for automatic cleanup.
   * <p>
   * This follows the pattern used in VertexCreationService but uses VertexLinker directly
   * to respect package boundaries (VertexCreationService is in the 'internal' package).
   *
   * @param location the location to get vertices for
   * @param linkingContext linking context to check for existing vertices
   * @return set of vertices for the location (either existing or newly created)
   */
  private Set<Vertex> getOrCreateVertices(GenericLocation location, LinkingContext linkingContext) {
    var vertices = linkingContext.findVertices(location);
    if (!vertices.isEmpty()) {
      return vertices;
    }

    var coordinate = location.getCoordinate();
    var tempVertex = new TemporaryStreetLocation(
      coordinate,
      new NonLocalizedString(location.label != null ? location.label : "Waypoint")
    );

    var disposableEdges = vertexLinker.linkVertexForRequest(
      tempVertex,
      new TraverseModeSet(TraverseMode.CAR),
      LinkingDirection.BIDIRECTIONAL,
      (vertex, streetVertex) ->
        List.of(
          TemporaryFreeEdge.createTemporaryFreeEdge((TemporaryVertex) vertex, streetVertex),
          TemporaryFreeEdge.createTemporaryFreeEdge(streetVertex, (TemporaryVertex) vertex)
        )
    );

    // Add to container for automatic cleanup
    temporaryVerticesContainer.addEdgeCollection(disposableEdges);

    if (tempVertex.getIncoming().isEmpty() && tempVertex.getOutgoing().isEmpty()) {
      LOG.warn("Couldn't link coordinate {} to graph for location {}", coordinate, location);
    } else {
      LOG.debug("Created temporary vertex for coordinate {} (not in LinkingContext)", coordinate);
    }

    return Set.of(tempVertex);
  }

  /**
   * Core A* routing for carpooling optimized for car travel.
   * <p>
   * Configures and executes an A* street search with settings optimized for carpooling:
   * <ul>
   *   <li><strong>Heuristic:</strong> Euclidean distance with max car speed</li>
   *   <li><strong>Skip Strategy:</strong> Duration-based edge skipping</li>
   *   <li><strong>Dominance:</strong> Minimum weight</li>
   *   <li><strong>Sorting:</strong> Results sorted by arrival/departure time</li>
   * </ul>
   *
   * @param streetRequest the street request specifying CAR mode
   * @param fromVertices set of origin vertices
   * @param toVertices set of destination vertices
   * @param maxCarSpeed maximum car speed in m/s
   * @return the first (best) path found, or null if no paths exist
   */
  private GraphPath<State, Edge, Vertex> carpoolRouting(
    StreetRequest streetRequest,
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices,
    float maxCarSpeed
  ) {
    var preferences = request.preferences().street();

    var streetSearch = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic(maxCarSpeed))
      .withSkipEdgeStrategy(
        new DurationSkipEdgeStrategy(preferences.maxDirectDuration().valueOf(streetRequest.mode()))
      )
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withRequest(request)
      .withStreetRequest(streetRequest)
      .withFrom(fromVertices)
      .withTo(toVertices);

    List<GraphPath<State, Edge, Vertex>> paths = streetSearch.getPathsToTarget();
    paths.sort(new PathComparator(request.arriveBy()));

    if (paths.isEmpty()) {
      return null;
    }

    return paths.getFirst();
  }
}
