package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.StationCentroidVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * This represents the connection between a street vertex and a transit station centroid vertex
 */
public class StreetStationCentroidLink extends FreeEdge {

  private StreetStationCentroidLink(Vertex fromv, StationCentroidVertex tov) {
    super(fromv, tov);
  }

  private StreetStationCentroidLink(StationCentroidVertex fromv, Vertex tov) {
    super(fromv, tov);
  }

  public static StreetStationCentroidLink createStreetStationLink(
    Vertex fromv,
    StationCentroidVertex tov
  ) {
    return connectToGraph(new StreetStationCentroidLink(fromv, tov));
  }

  public static StreetStationCentroidLink createStreetStationLink(
    StationCentroidVertex fromv,
    Vertex tov
  ) {
    return connectToGraph(new StreetStationCentroidLink(fromv, tov));
  }
}
