package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.site.PathwayNode;
import org.opentripplanner.transit.model.site.StationElement;

public class TransitPathwayNodeVertex extends StationElementVertex {

  private final boolean wheelchairEntrance;

  private final PathwayNode node;

  /**
   * @param node The transit model pathway node reference.
   */
  public TransitPathwayNodeVertex(PathwayNode node) {
    super(
      node.getId(),
      node.getCoordinate().longitude(),
      node.getCoordinate().latitude(),
      node.getName()
    );
    this.node = node;
    this.wheelchairEntrance = node.getWheelchairAccessibility() != Accessibility.NOT_POSSIBLE;
  }

  public boolean isWheelchairEntrance() {
    return wheelchairEntrance;
  }

  public PathwayNode getNode() {
    return this.node;
  }

  @Nonnull
  @Override
  public StationElement getStationElement() {
    return this.node;
  }
}
