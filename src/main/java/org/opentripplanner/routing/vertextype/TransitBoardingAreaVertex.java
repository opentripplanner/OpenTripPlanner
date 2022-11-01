package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.StationElement;

public class TransitBoardingAreaVertex extends Vertex {

  private static final long serialVersionUID = 1L;

  private final boolean wheelchairAccessible;

  private final BoardingArea boardingArea;

  /**
   * @param boardingArea The transit model boarding area reference.
   */
  public TransitBoardingAreaVertex(Graph graph, BoardingArea boardingArea) {
    super(
      graph,
      boardingArea.getId().toString(),
      boardingArea.getCoordinate().longitude(),
      boardingArea.getCoordinate().latitude(),
      boardingArea.getName()
    );
    this.boardingArea = boardingArea;
    this.wheelchairAccessible =
      boardingArea.getWheelchairAccessibility() != Accessibility.NOT_POSSIBLE;
    //Adds this vertex into graph envelope so that we don't need to loop over all vertices
    graph.expandToInclude(
      boardingArea.getCoordinate().longitude(),
      boardingArea.getCoordinate().latitude()
    );
  }

  public boolean isWheelchairAccessible() {
    return wheelchairAccessible;
  }

  public BoardingArea getBoardingArea() {
    return this.boardingArea;
  }

  @Override
  public StationElement getStationElement() {
    return this.boardingArea;
  }
}
