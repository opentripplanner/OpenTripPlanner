package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.StationElement;

public class TransitBoardingAreaVertex extends StationElementVertex {

  private final boolean wheelchairAccessible;

  private final BoardingArea boardingArea;

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
    this.boardingArea = boardingArea;
    this.wheelchairAccessible =
      boardingArea.getWheelchairAccessibility() != Accessibility.NOT_POSSIBLE;
  }

  public boolean isWheelchairAccessible() {
    return wheelchairAccessible;
  }

  @Nonnull
  @Override
  public StationElement getStationElement() {
    return this.boardingArea;
  }
}
