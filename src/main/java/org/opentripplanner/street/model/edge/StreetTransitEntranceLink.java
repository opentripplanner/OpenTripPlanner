package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;

/**
 * This represents the connection between a street vertex and a transit vertex belonging the street
 * network.
 */
public class StreetTransitEntranceLink extends StreetTransitEntityLink<TransitEntranceVertex> {

  private StreetTransitEntranceLink(StreetVertex fromv, TransitEntranceVertex tov) {
    super(fromv, tov, tov.getWheelchairAccessibility());
  }

  private StreetTransitEntranceLink(TransitEntranceVertex fromv, StreetVertex tov) {
    super(fromv, tov, fromv.getWheelchairAccessibility());
  }

  public static StreetTransitEntranceLink createStreetTransitEntranceLink(
    StreetVertex fromv,
    TransitEntranceVertex tov
  ) {
    return connectToGraph(new StreetTransitEntranceLink(fromv, tov));
  }

  public static StreetTransitEntranceLink createStreetTransitEntranceLink(
    TransitEntranceVertex fromv,
    StreetVertex tov
  ) {
    return connectToGraph(new StreetTransitEntranceLink(fromv, tov));
  }

  public String toString() {
    return "StreetTransitEntranceLink(" + fromv + " -> " + tov + ")";
  }

  protected int getStreetToStopTime() {
    return 0;
  }
}
