package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStationCentroidVertex;
import org.opentripplanner.transit.model.basic.Accessibility;

/**
 * This represents the connection between a street vertex and a transit station centroid vertex
 */
public class StreetTransitStationCentroidLink
  extends StreetTransitEntityLink<TransitStationCentroidVertex> {

  private StreetTransitStationCentroidLink(StreetVertex fromv, TransitStationCentroidVertex tov) {
    super(fromv, tov, tov.getStation().getWheelchairAccessibility());
  }

  private StreetTransitStationCentroidLink(TransitStationCentroidVertex fromv, StreetVertex tov) {
    super(fromv, tov, fromv.getStation().getWheelchairAccessibility());
  }

  public static StreetTransitStationCentroidLink createStreetTransitStationLink(
    StreetVertex fromv,
    TransitStationCentroidVertex tov
  ) {
    return connectToGraph(new StreetTransitStationCentroidLink(fromv, tov));
  }

  public static StreetTransitStationCentroidLink createStreetTransitStationLink(
    TransitStationCentroidVertex fromv,
    StreetVertex tov
  ) {
    return connectToGraph(new StreetTransitStationCentroidLink(fromv, tov));
  }

  public String toString() {
    return "StreetTransitStationCentroidLink(" + fromv + " -> " + tov + ")";
  }

  protected int getStreetToStopTime() {
    return 0;
  }
}
