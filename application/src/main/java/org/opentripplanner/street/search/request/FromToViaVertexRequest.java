package org.opentripplanner.street.search.request;

import java.util.Map;
import java.util.Set;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class FromToViaVertexRequest {

  private final Set<Vertex> from;
  private final Set<TransitStopVertex> fromStops;
  private final Set<Vertex> to;
  private final Set<TransitStopVertex> toStops;
  private final Map<VisitViaLocation, Set<Vertex>> visitViaLocationVertices;

  public FromToViaVertexRequest(Set<Vertex> from) {
    this.from = from;
    this.fromStops = Set.of();
    this.to = Set.of();
    this.toStops = Set.of();
    this.visitViaLocationVertices = Map.of();
  }

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

  public Set<Vertex> from() {
    return from;
  }

  /**
   * Get the stop vertices that correspond to the from location. If the from location only contains
   * coordinates, this will return an empty set. If the from location is a station id this will
   * return the child stops of that station.
   */
  public Set<TransitStopVertex> fromStops() {
    return fromStops;
  }

  public Set<Vertex> to() {
    return to;
  }

  /**
   * Get the stop vertices that corresponds to the to location. If the to location only contains
   * coordinates, this will return an empty set. If the to location is a station id this will
   * return the child stops of that station.
   */
  public Set<TransitStopVertex> toStops() {
    return toStops;
  }

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
