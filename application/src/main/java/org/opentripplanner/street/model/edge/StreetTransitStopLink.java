package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * This represents the connection between a street vertex and a transit vertex where going from the
 * street to the vehicle is immediate -- such as at a curbside bus stop.
 */
public class StreetTransitStopLink extends StreetTransitEntityLink<TransitStopVertex> {

  private StreetTransitStopLink(Vertex fromv, TransitStopVertex tov) {
    super(fromv, tov, tov.getWheelchairAccessibility());
  }

  private StreetTransitStopLink(TransitStopVertex fromv, Vertex tov) {
    super(fromv, tov, fromv.getWheelchairAccessibility());
  }

  public static StreetTransitStopLink createStreetTransitStopLink(
    Vertex fromv,
    TransitStopVertex tov
  ) {
    return connectToGraph(new StreetTransitStopLink(fromv, tov));
  }

  public static StreetTransitStopLink createStreetTransitStopLink(
    TransitStopVertex fromv,
    Vertex tov
  ) {
    return connectToGraph(new StreetTransitStopLink(fromv, tov));
  }

  protected int getStreetToStopTime() {
    return ((TransitStopVertex) getTransitEntityVertex()).hasPathways()
      ? 0
      : ((TransitStopVertex) getTransitEntityVertex()).getStreetToStopTime();
  }
}
