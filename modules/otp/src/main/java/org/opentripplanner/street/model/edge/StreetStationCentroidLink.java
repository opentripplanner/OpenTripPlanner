package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.StationCentroidVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;

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
}
