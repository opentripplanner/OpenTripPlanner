package org.opentripplanner.routing.vertextype;

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
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
        GtfsLibrary.convertIdToString(node.getId()),
        node.getLon(),
        node.getLat()
    );
    this.node = node;
    this.wheelchairEntrance = node.getWheelchairBoarding() != WheelChairBoarding.NOT_POSSIBLE;
    //Adds this vertex into graph envelope so that we don't need to loop over all vertices
    graph.expandToInclude(node.getLon(), node.getLat());
  }

  @Override
  public String getName() {
    return node.getName();
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
