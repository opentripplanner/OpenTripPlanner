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
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Holds vertices (mainly temporary) that are meant to be used within the scope of a single route
 * request that can contain access/egress/direct/transfer routing. The temporary vertices will be
 * disposed by {@link org.opentripplanner.street.search.TemporaryVerticesContainer} after the search
 * is over.
 */
public class LinkingContext {

  private final GenericLocation from;
  private final GenericLocation to;
  private final Map<GenericLocation, Set<Vertex>> verticesByLocation;
  private final Set<TransitStopVertex> fromStopVertices;
  private final Set<TransitStopVertex> toStopVertices;

  public LinkingContext(
    GenericLocation from,
    GenericLocation to,
    Map<GenericLocation, Set<Vertex>> verticesByLocation,
    Set<TransitStopVertex> fromStopVertices,
    Set<TransitStopVertex> toStopVertices
  ) {
    this.from = from;
    this.fromStopVertices = fromStopVertices;
    this.toStopVertices = toStopVertices;
    this.to = to;
    this.verticesByLocation = verticesByLocation;
  }

  public LinkingContext(LinkingContextBuilder builder) {
    this(
      builder.from(),
      builder.to(),
      builder.verticesByLocation(),
      builder.fromStopVertices(),
      builder.toStopVertices()
    );
  }

  /**
   * Create builder when stop locations are not used for locations.
   */
  public static LinkingContextBuilder of(
    TemporaryVerticesContainer container,
    Graph graph,
    VertexLinker linker
  ) {
    return new LinkingContextBuilder(container, graph, linker, id -> Set.of());
  }

  /**
   * Create builder when stop locations are potentially used for locations.
   */
  public static LinkingContextBuilder of(
    TemporaryVerticesContainer container,
    Graph graph,
    VertexLinker linker,
    Function<FeedScopedId, Collection<FeedScopedId>> resolveSiteIds
  ) {
    return new LinkingContextBuilder(container, graph, linker, resolveSiteIds);
  }

  public static LinkingContext ofForTest(
    GenericLocation from,
    GenericLocation to,
    Map<GenericLocation, Set<Vertex>> verticesByLocation,
    Set<TransitStopVertex> fromStopVertices,
    Set<TransitStopVertex> toStopVertices
  ) {
    return new LinkingContext(from, to, verticesByLocation, fromStopVertices, toStopVertices);
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
   * Vertices that are used for either origin, destination or for via locations. Only the visit via
   * locations that have a coordinate specified will have vertices available. Stop vertices are not
   * included via locations.
   */
  public Set<Vertex> findVertices(GenericLocation location) {
    return verticesByLocation.getOrDefault(location, Set.of());
  }
}
