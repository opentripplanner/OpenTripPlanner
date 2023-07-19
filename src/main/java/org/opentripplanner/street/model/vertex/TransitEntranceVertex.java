package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.StationElement;

public class TransitEntranceVertex extends StationElementVertex {

  private final Accessibility wheelchairAccessibility;

  private final Entrance entrance;

  /**
   * @param entrance The transit model entrance reference.
   */
  public TransitEntranceVertex(Entrance entrance) {
    super(
      entrance.getId(),
      entrance.getCoordinate().longitude(),
      entrance.getCoordinate().latitude(),
      entrance.getName()
    );
    this.entrance = entrance;
    this.wheelchairAccessibility = entrance.getWheelchairAccessibility();
  }

  public Accessibility getWheelchairAccessibility() {
    return wheelchairAccessibility;
  }

  public Entrance getEntrance() {
    return this.entrance;
  }

  @Nonnull
  @Override
  public StationElement getStationElement() {
    return this.entrance;
  }
}
