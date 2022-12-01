package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.site.PathwayNode;
import org.opentripplanner.transit.model.site.StationElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitPathwayNodeVertex extends StationElementVertex {

  private static final Logger LOG = LoggerFactory.getLogger(TransitPathwayNodeVertex.class);

  private static final long serialVersionUID = 1L;

  private final boolean wheelchairEntrance;

  private final PathwayNode node;

  /**
   * @param node The transit model pathway node reference.
   */
  public TransitPathwayNodeVertex(Graph graph, PathwayNode node) {
    super(
      graph,
      node.getId().toString(),
      node.getCoordinate().longitude(),
      node.getCoordinate().latitude(),
      node.getName()
    );
    this.node = node;
    this.wheelchairEntrance = node.getWheelchairAccessibility() != Accessibility.NOT_POSSIBLE;
    //Adds this vertex into graph envelope so that we don't need to loop over all vertices
    graph.expandToInclude(node.getCoordinate().longitude(), node.getCoordinate().latitude());
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
