package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.Entrance;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

public class TransitEntranceVertex extends Vertex {

  private static final long serialVersionUID = 1L;

  private final WheelChairBoarding wheelchairBoarding;

  private final Entrance entrance;

  /**
   * @param entrance The transit model entrance reference.
   */
  public TransitEntranceVertex(Graph graph, Entrance entrance) {
    super(
      graph,
      entrance.getId().toString(),
      entrance.getCoordinate().longitude(),
      entrance.getCoordinate().latitude(),
      entrance.getName()
    );
    this.entrance = entrance;
    this.wheelchairBoarding = entrance.getWheelchairBoarding();
    //Adds this vertex into graph envelope so that we don't need to loop over all vertices
    graph.expandToInclude(
      entrance.getCoordinate().longitude(),
      entrance.getCoordinate().latitude()
    );
  }

  public WheelChairBoarding getWheelchairBoarding() {
    return wheelchairBoarding;
  }

  public Entrance getEntrance() {
    return this.entrance;
  }

  @Override
  public StationElement getStationElement() {
    return this.entrance;
  }
}
