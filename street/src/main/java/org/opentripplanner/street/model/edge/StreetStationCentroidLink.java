package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.StationCentroidVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * This represents the connection between a street vertex and a transit station centroid vertex
 */
public class StreetStationCentroidLink extends FreeEdge {

  private StreetStationCentroidLink(StreetVertex fromv, StationCentroidVertex tov) {
    super(fromv, tov);
  }

  private StreetStationCentroidLink(StationCentroidVertex fromv, StreetVertex tov) {
    super(fromv, tov);
  }

  public static StreetStationCentroidLink createStreetStationLink(
    StreetVertex fromv,
    StationCentroidVertex tov
  ) {
    return connectToGraph(new StreetStationCentroidLink(fromv, tov));
  }

  public static StreetStationCentroidLink createStreetStationLink(
    StationCentroidVertex fromv,
    StreetVertex tov
  ) {
    return connectToGraph(new StreetStationCentroidLink(fromv, tov));
  }

  /**
   * Either from or to needs to be a {@link StationCentroidVertex} and the other vertex should be a
   * {@link StreetVertex}.
   */
  public static StreetStationCentroidLink createStreetStationLink(Vertex from, Vertex to) {
    if (
      from instanceof StationCentroidVertex stationCentroid && to instanceof StreetVertex street
    ) {
      return connectToGraph(new StreetStationCentroidLink(stationCentroid, street));
    }
    if (
      to instanceof StationCentroidVertex stationCentroid && from instanceof StreetVertex street
    ) {
      return connectToGraph(new StreetStationCentroidLink(street, stationCentroid));
    }
    throw new IllegalArgumentException(
      "Vertices need to be a station centroid vertex and a street vertex. Got: " +
        from.getClass() +
        " and " +
        to.getClass()
    );
  }
}
