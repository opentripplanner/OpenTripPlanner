package org.opentripplanner.street.model.vertex;

import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.site.BoardingArea;

public class TransitBoardingAreaVertex extends StationElementVertex {

  private final boolean wheelchairAccessible;

  /**
   * @param boardingArea The transit model boarding area reference.
   */
  public TransitBoardingAreaVertex(BoardingArea boardingArea) {
    super(
      boardingArea.getId(),
      boardingArea.getCoordinate().longitude(),
      boardingArea.getCoordinate().latitude(),
      boardingArea.getName()
    );
    this.wheelchairAccessible =
      boardingArea.getWheelchairAccessibility() != Accessibility.NOT_POSSIBLE;
  }

  public boolean isWheelchairAccessible() {
    return wheelchairAccessible;
  }
}
