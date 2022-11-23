package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;

/**
 * This represents the connection between a street vertex and a transit vertex belonging the street
 * network.
 */
public class StreetTransitEntranceLink extends StreetTransitEntityLink<TransitEntranceVertex> {

  public StreetTransitEntranceLink(StreetVertex fromv, TransitEntranceVertex tov) {
    super(fromv, tov, tov.getWheelchairAccessibility());
  }

  public StreetTransitEntranceLink(TransitEntranceVertex fromv, StreetVertex tov) {
    super(fromv, tov, fromv.getWheelchairAccessibility());
  }

  public String toString() {
    return "StreetTransitEntranceLink(" + fromv + " -> " + tov + ")";
  }

  protected int getStreetToStopTime() {
    return 0;
  }
}
