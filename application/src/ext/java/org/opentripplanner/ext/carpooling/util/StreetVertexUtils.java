package org.opentripplanner.ext.carpooling.util;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.linking.LinkingDirection;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for resolving street graph vertices from geographic locations within the carpooling
 * routing context.
 * <p>
 * This class provides a two-step vertex resolution strategy:
 * <ol>
 *   <li>Check the {@link LinkingContext} for pre-linked vertices (e.g., the passenger's origin
 *       and destination, which are linked as part of the standard OTP request lifecycle).</li>
 *   <li>If not found, create a temporary vertex on-demand using {@link VertexLinker} and register
 *       it with a {@link TemporaryVerticesContainer} for automatic cleanup after the search.</li>
 * </ol>
 * <p>
 * The on-demand creation path is needed for driver trip waypoints, which are not part of the
 * original routing request and therefore have no pre-linked vertices.
 */
public class StreetVertexUtils {

  private static final Logger LOG = LoggerFactory.getLogger(StreetVertexUtils.class);

  private final VertexLinker vertexLinker;
  private final TemporaryVerticesContainer temporaryVerticesContainer;

  /**
   * @param vertexLinker links coordinates to graph vertices
   * @param temporaryVerticesContainer container for temporary vertices and edges
   */
  public StreetVertexUtils(
    VertexLinker vertexLinker,
    TemporaryVerticesContainer temporaryVerticesContainer
  ) {
    this.vertexLinker = vertexLinker;
    this.temporaryVerticesContainer = temporaryVerticesContainer;
  }

  /**
   * Gets a vertex for a coordinate, either from the LinkingContext or by creating
   * a temporary vertex on-demand.
   * <p>
   * This method first checks if vertices already exist in the LinkingContext (which
   * contains pre-linked vertices for the passenger's origin and destination). If not
   * found (e.g., for driver trip waypoints), it creates a temporary vertex on-demand
   * using VertexLinker and adds it to the TemporaryVerticesContainer for automatic cleanup.
   * <p>
   * @param coord the coordinate to get a vertex for
   * @param linkingContext linking context to check for existing vertices
   * @return vertex for the coordinate, or null if it could not be linked to the graph
   */
  @Nullable
  public Vertex getOrCreateVertex(WgsCoordinate coord, LinkingContext linkingContext) {
    var location = GenericLocation.fromCoordinate(coord.latitude(), coord.longitude());
    var vertices = linkingContext.findVertices(location);
    if (!vertices.isEmpty()) {
      return vertices.stream().findFirst().get();
    }

    var tempVertex = new TemporaryStreetLocation(
      coord.asJtsCoordinate(),
      new NonLocalizedString("Carpooling Waypoint")
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
      LOG.error("Couldn't link coordinate {} to graph", coord);
      return null;
    }

    LOG.debug("Created temporary vertex for coordinate {} (not in LinkingContext)", coord);
    return tempVertex;
  }
}
