package org.opentripplanner.street.search.request;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.model.GenericLocation;
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

  private final Set<TransitStopVertex> fromStops;
  private final Set<TransitStopVertex> toStops;
  private final Map<GenericLocation, Set<Vertex>> verticesByLocation;

  public FromToViaVertexRequest(
    Set<TransitStopVertex> fromStops,
    Set<TransitStopVertex> toStops,
    Map<GenericLocation, Set<Vertex>> verticesByLocation
  ) {
    this.fromStops = fromStops;
    this.toStops = toStops;
    this.verticesByLocation = verticesByLocation;
  }

  /**
   * If from location (origin) has a stop id defined, this will include vertices related to it. If
   * the stop is a station, the child stops will be included.
   */
  public Set<TransitStopVertex> fromStops() {
    return fromStops;
  }

  /**
   * If to location (destination) has a stop id defined, this will include vertices related to it.
   * If the stop is a station, the child stops will be included.
   */
  public Set<TransitStopVertex> toStops() {
    return toStops;
  }

  /**
   * Vertices that are used for either origin, destination or for via locations. Only the visit via
   * locations that have a coordinate specified will have vertices available. Stop vertices are not
   * included via locations.
   */
  public Set<Vertex> findVertices(GenericLocation location) {
    return verticesByLocation.getOrDefault(location, Set.of());
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(FromToViaVertexRequest.class)
      .addCol("fromStops", fromStops, Set.of())
      .addCol("toStops", toStops, Set.of())
      .addCol(
        "verticesByLocation",
        verticesByLocation
          .entrySet()
          .stream()
          .sorted(Map.Entry.comparingByKey(Comparator.comparing(GenericLocation::hashCode)))
          .toList(),
        List.of()
      )
      .toString();
  }
}
