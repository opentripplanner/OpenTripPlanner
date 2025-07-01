package org.opentripplanner.street.search.request;

import java.util.Map;
import java.util.Set;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Holds vertices (mainly temporary) that are meant to be used within the scope of a single route
 * request that can contain access/egress/direct/transfer routing. The temporary vertices will be
 * disposed by {@link org.opentripplanner.street.search.TemporaryVerticesContainer} after the search
 * is over.
 */
public class FromToViaVertexRequest {

  private final Set<Vertex> from;
  private final Set<TransitStopVertex> fromStops;
  private final Set<Vertex> to;
  private final Set<TransitStopVertex> toStops;
  private final Map<VisitViaLocation, Set<Vertex>> visitViaLocationVertices;

  public FromToViaVertexRequest(
    Set<Vertex> from,
    Set<Vertex> to,
    Set<TransitStopVertex> fromStops,
    Set<TransitStopVertex> toStops,
    Map<VisitViaLocation, Set<Vertex>> visitViaLocationVertices
  ) {
    this.from = from;
    this.fromStops = fromStops;
    this.to = to;
    this.toStops = toStops;
    this.visitViaLocationVertices = visitViaLocationVertices;
  }

  /**
   * Vertices that are used for from (origin). This includes both street and stop vertices.
   */
  public Set<Vertex> from() {
    return from;
  }

  /**
   * If from location (origin) has a stop id defined, this will include vertices related to it. If
   * the stop is a station, the child stops will be included.
   */
  public Set<TransitStopVertex> fromStops() {
    return fromStops;
  }

  /**
   * Vertices that are used for to (destination). This includes both street and stop vertices.
   */
  public Set<Vertex> to() {
    return to;
  }

  /**
   * If to location (destination) has a stop id defined, this will include vertices related to it.
   * If the stop is a station, the child stops will be included.
   */
  public Set<TransitStopVertex> toStops() {
    return toStops;
  }

  /**
   * Vertices that are used for a visit via location. Only the visit via locations that have a
   * coordinate specified will have vertices available. Stop vertices are not included.
   */
  public Set<Vertex> findVertices(VisitViaLocation visitViaLocation) {
    return visitViaLocationVertices.getOrDefault(visitViaLocation, Set.of());
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(FromToViaVertexRequest.class)
      .addCol("from", from, Set.of())
      .addCol("fromStops", fromStops, Set.of())
      .addCol("to", to, Set.of())
      .addCol("toStops", toStops, Set.of())
      .addObj("visitViaLocationVertices", visitViaLocationVertices, Map.of())
      .toString();
  }
}
