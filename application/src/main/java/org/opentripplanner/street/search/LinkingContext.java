package org.opentripplanner.street.search;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.FromToViaVertexRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This class contains temporary vertices and edges that are used in A-Star searches. After they
 * are no longer needed, this class removes the temporary vertices and edges. It implements
 * AutoCloseable and the cleanup is automatically done with a try-with-resources statement.
 */
public class LinkingContext {

  private final GenericLocation from;
  private final GenericLocation to;
  private final Map<GenericLocation, Set<Vertex>> verticesByLocation;
  private final Set<TransitStopVertex> fromStopVertices;
  private final Set<TransitStopVertex> toStopVertices;

  public LinkingContext(TemporaryVerticesContainerBuilder builder) {
    this.from = builder.from();
    this.to = builder.to();
    this.fromStopVertices = builder.fromStopVertices();
    this.verticesByLocation = builder.verticesByLocation();
    this.toStopVertices = builder.toStopVertices();
  }

  /**
   * Create builder when stop locations are not used for locations.
   */
  public static TemporaryVerticesContainerBuilder of(
    TemporaryVerticesContainer container,
    Graph graph, VertexLinker linker) {
    return new TemporaryVerticesContainerBuilder(container, graph, linker, id -> Set.of());
  }

  /**
   * Create builder when stop locations are potentially used for locations.
   */
  public static TemporaryVerticesContainerBuilder of(
    TemporaryVerticesContainer container,
    Graph graph,
    VertexLinker linker,
    Function<FeedScopedId, Collection<FeedScopedId>> resolveSiteIds
  ) {
    return new TemporaryVerticesContainerBuilder(container, graph, linker, resolveSiteIds);
  }

  /**
   * Vertices that are used for from (origin). This includes both street and stop vertices.
   */
  public Set<Vertex> fromVertices() {
    return verticesByLocation.getOrDefault(from, Set.of());
  }

  /**
   * Vertices that are used for to (destination). This includes both street and stop vertices.
   */
  public Set<Vertex> toVertices() {
    return verticesByLocation.getOrDefault(to, Set.of());
  }

  /**
   * Vertices that are used for either origin, destination or for via locations. Only the visit via
   * locations that have a coordinate specified will have vertices available. Stop vertices are not
   * included via locations.
   */
  public Map<GenericLocation, Set<Vertex>> verticesByLocation() {
    return verticesByLocation;
  }

  /**
   * If from location (origin) has a stop id defined, this will include vertices related to it. If
   * the stop is a station, the child stops will be included.
   */
  public Set<TransitStopVertex> fromStopVertices() {
    return fromStopVertices;
  }

  /**
   * If to location (destination) has a stop id defined, this will include vertices related to it.
   * If the stop is a station, the child stops will be included.
   */
  public Set<TransitStopVertex> toStopVertices() {
    return toStopVertices;
  }

  /**
   * Creates a {@link FromToViaVertexRequest} that contains vertices from this container.
   */
  public FromToViaVertexRequest createFromToViaVertexRequest() {
    return new FromToViaVertexRequest(fromStopVertices, toStopVertices, verticesByLocation);
  }
}
