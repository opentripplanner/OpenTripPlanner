package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This represents the connection between a street vertex and a transit vertex belonging the street
 * network.
 */
public class StreetTransitEntranceLink extends StreetTransitEntityLink<TransitEntranceVertex> {

  private final boolean isEntrance;

  private StreetTransitEntranceLink(StreetVertex fromv, TransitEntranceVertex tov) {
    super(fromv, tov, tov.getWheelchairAccessibility());
    isEntrance = true;
  }

  private StreetTransitEntranceLink(TransitEntranceVertex fromv, StreetVertex tov) {
    super(fromv, tov, fromv.getWheelchairAccessibility());
    isEntrance = false;
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

  public boolean isEntrance() {
    return isEntrance;
  }

  public boolean isExit() {
    return !isEntrance;
  }

  /**
   * Get the id of the entrance that this edge links to.
   */
  public FeedScopedId entrance() {
    if (getToVertex() instanceof TransitEntranceVertex tev) {
      return tev.getId();
    } else if (getFromVertex() instanceof TransitEntranceVertex tev) {
      return tev.getId();
    }
    throw new IllegalStateException("%s doesn't link to an entrance.".formatted(this));
  }

  protected int getStreetToStopTime() {
    return 0;
  }
}
