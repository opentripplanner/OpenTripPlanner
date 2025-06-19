package org.opentripplanner.routing.api.request;

import java.util.Set;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class FromToViaVertexRequest {

  private final Set<Vertex> from;
  private final Set<TransitStopVertex> fromStops;
  private final Set<Vertex> to;
  private final Set<TransitStopVertex> toStops;

  public FromToViaVertexRequest(Set<Vertex> from) {
    this.from = from;
    this.fromStops = Set.of();
    this.to = Set.of();
    this.toStops = Set.of();
  }

  public FromToViaVertexRequest(
    Set<Vertex> from,
    Set<Vertex> to,
    Set<TransitStopVertex> fromStops,
    Set<TransitStopVertex> toStops
  ) {
    this.from = from;
    this.fromStops = fromStops;
    this.to = to;
    this.toStops = toStops;
  }

  public Set<Vertex> from() {
    return from;
  }

  public Set<TransitStopVertex> fromStops() {
    return fromStops;
  }

  public Set<Vertex> to() {
    return to;
  }

  public Set<TransitStopVertex> toStops() {
    return toStops;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(FromToViaVertexRequest.class)
      .addCol("from", from, Set.of())
      .addCol("fromStops", fromStops, Set.of())
      .addCol("to", to, Set.of())
      .addCol("toStops", toStops, Set.of())
      .toString();
  }
}
