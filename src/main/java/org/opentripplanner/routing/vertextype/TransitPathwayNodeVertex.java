package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitPathwayNodeVertex extends Vertex {

  private static final Logger LOG = LoggerFactory.getLogger(TransitPathwayNodeVertex.class);

  private static final long serialVersionUID = 1L;

  private boolean wheelchairEntrance;

  private PathwayNode node;

  /**
   * @param node The transit model pathway node reference.
   */
  public TransitPathwayNodeVertex(Graph graph, PathwayNode node) {
    super(
        graph,
        node.getId().toString(),
        node.getCoordinate().longitude(),
        node.getCoordinate().latitude(),
        new NonLocalizedString(node.getName())
    );
    this.node = node;
    this.wheelchairEntrance = node.getWheelchairBoarding() != WheelChairBoarding.NOT_POSSIBLE;
    //Adds this vertex into graph envelope so that we don't need to loop over all vertices
    graph.expandToInclude(node.getCoordinate().longitude(), node.getCoordinate().latitude());
  }

  public boolean isWheelchairEntrance() {
    return wheelchairEntrance;
  }

  public PathwayNode getNode() {
    return this.node;
  }

  @Override
  public StationElement getStationElement() {
    return this.node;
  }
}
