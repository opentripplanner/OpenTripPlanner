package org.opentripplanner.routing.vertextype;

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.Entrance;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitEntranceVertex extends Vertex {

  private static final Logger LOG = LoggerFactory.getLogger(TransitEntranceVertex.class);

  private static final long serialVersionUID = 1L;

  private boolean wheelchairEntrance;

  private Entrance entrance;

  /**
   * @param entrance The transit model entrance reference.
   */
  public TransitEntranceVertex(Graph graph, Entrance entrance) {
    super(
        graph,
        GtfsLibrary.convertIdToString(entrance.getId()),
        entrance.getLon(),
        entrance.getLat()
    );
    this.entrance = entrance;
    this.wheelchairEntrance = entrance.getWheelchairBoarding() != WheelChairBoarding.NOT_POSSIBLE;
    //Adds this vertex into graph envelope so that we don't need to loop over all vertices
    graph.expandToInclude(entrance.getLon(), entrance.getLat());
  }

  @Override
  public String getName() {
    return entrance.getName();
  }

  public boolean isWheelchairEntrance() {
    return wheelchairEntrance;
  }

  public Entrance getEntrance() {
    return this.entrance;
  }

  @Override
  public StationElement getStationElement() {
    return this.entrance;
  }
}
