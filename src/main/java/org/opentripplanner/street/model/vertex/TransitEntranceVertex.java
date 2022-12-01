package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.StationElement;

public class TransitEntranceVertex extends StationElementVertex {

  private static final long serialVersionUID = 1L;

  private final Accessibility wheelchairAccessibility;

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
    this.wheelchairAccessibility = entrance.getWheelchairAccessibility();
    //Adds this vertex into graph envelope so that we don't need to loop over all vertices
    graph.expandToInclude(
      entrance.getCoordinate().longitude(),
      entrance.getCoordinate().latitude()
    );
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
