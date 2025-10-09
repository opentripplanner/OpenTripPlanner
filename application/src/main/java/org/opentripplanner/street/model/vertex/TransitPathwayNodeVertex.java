package org.opentripplanner.street.model.vertex;

import org.opentripplanner.transit.model.site.PathwayNode;

public class TransitPathwayNodeVertex extends StationElementVertex {

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
  }

  public PathwayNode getNode() {
    return this.node;
  }
}
