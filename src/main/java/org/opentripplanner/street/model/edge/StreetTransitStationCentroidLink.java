package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStationCentroidVertex;
import org.opentripplanner.transit.model.basic.Accessibility;

public class StreetTransitStationCentroidLink
  extends StreetTransitEntityLink<TransitStationCentroidVertex> {

  private final boolean isEntrance;

  private StreetTransitStationCentroidLink(StreetVertex fromv, TransitStationCentroidVertex tov) {
    super(fromv, tov, tov.getStation().getWheelchairAccessibility());
    isEntrance = true;
  }

  private StreetTransitStationCentroidLink(TransitStationCentroidVertex fromv, StreetVertex tov) {
    super(fromv, tov, fromv.getStation().getWheelchairAccessibility());
    isEntrance = false;
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

  public boolean isEntrance() {
    return isEntrance;
  }

  public boolean isExit() {
    return !isEntrance;
  }

  public String toString() {
    return "StreetTransitStationCentroidLink(" + fromv + " -> " + tov + ")";
  }

  protected int getStreetToStopTime() {
    return 0;
  }
}
