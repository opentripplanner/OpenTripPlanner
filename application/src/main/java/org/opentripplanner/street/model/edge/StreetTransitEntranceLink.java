package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * This represents the connection between a street vertex and a transit vertex belonging the street
 * network.
 */
public class StreetTransitEntranceLink extends StreetTransitEntityLink<TransitEntranceVertex> {

  private final boolean isEntrance;

  private StreetTransitEntranceLink(Vertex fromv, TransitEntranceVertex tov) {
    super(fromv, tov, tov.getWheelchairAccessibility());
    isEntrance = true;
  }

  private StreetTransitEntranceLink(TransitEntranceVertex fromv, Vertex tov) {
    super(fromv, tov, fromv.getWheelchairAccessibility());
    isEntrance = false;
  }

  public static StreetTransitEntranceLink createStreetTransitEntranceLink(
    Vertex fromv,
    TransitEntranceVertex tov
  ) {
    return connectToGraph(new StreetTransitEntranceLink(fromv, tov));
  }

  public static StreetTransitEntranceLink createStreetTransitEntranceLink(
    TransitEntranceVertex fromv,
    Vertex tov
  ) {
    return connectToGraph(new StreetTransitEntranceLink(fromv, tov));
  }

  public boolean isEntrance() {
    return isEntrance;
  }

  public boolean isExit() {
    return !isEntrance;
  }

  protected int getStreetToStopTime() {
    return 0;
  }
}
