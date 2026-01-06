package org.opentripplanner.street.model.edge;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.street.model.vertex.ElevatorHopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * A relatively high cost edge for boarding an elevator.
 *
 * @author mattwigway
 */
public class ElevatorBoardEdge extends Edge implements BikeWalkableEdge, ElevatorEdge {

  /**
   * The polyline geometry of this edge. It's generally a polyline with two coincident points, but
   * some elevators have horizontal dimension, e.g. the ones on the Eiffel Tower.
   */
  private final LineString geometry;

  private ElevatorBoardEdge(Vertex from, ElevatorHopVertex to) {
    super(from, to);
    geometry = GeometryUtils.makeLineString(
      List.of(new Coordinate(from.getX(), from.getY()), new Coordinate(to.getX(), to.getY()))
    );
  }

  public static ElevatorBoardEdge createElevatorBoardEdge(Vertex from, ElevatorHopVertex to) {
    return connectToGraph(new ElevatorBoardEdge(from, to));
  }

  @Override
  public State[] traverse(State s0) {
    StateEditor s1 = createEditorForDrivingOrWalking(s0, this);
    if (s1 == null) {
      return State.empty();
    }

    var req = s0.getRequest();

    s1.incrementWeight(req.elevator().boardCost());
    s1.incrementTimeInSeconds(req.elevator().boardTime());

    return s1.makeStateArray();
  }

  @Override
  public I18NString getName() {
    // TODO: i18n
    return new NonLocalizedString("ElevatorBoardEdge");
  }

  /**
   * Since board edges are always called ElevatorBoardEdge, the name is complete bogus but is
   * never included in plans.
   */
  @Override
  public boolean nameIsDerived() {
    return true;
  }

  @Override
  public LineString getGeometry() {
    return geometry;
  }
}
