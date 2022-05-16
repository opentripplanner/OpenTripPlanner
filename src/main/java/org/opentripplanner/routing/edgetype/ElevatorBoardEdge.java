package org.opentripplanner.routing.edgetype;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.WheelchairBoarding;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.ElevatorOffboardVertex;
import org.opentripplanner.routing.vertextype.ElevatorOnboardVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * A relatively high cost edge for boarding an elevator.
 *
 * @author mattwigway
 */
public class ElevatorBoardEdge extends Edge implements BikeWalkableEdge, ElevatorEdge {

  private static final long serialVersionUID = 3925814840369402222L;

  /**
   * The polyline geometry of this edge. It's generally a polyline with two coincident points, but
   * some elevators have horizontal dimension, e.g. the ones on the Eiffel Tower.
   */
  private final LineString geometry;

  public ElevatorBoardEdge(ElevatorOffboardVertex from, ElevatorOnboardVertex to) {
    super(from, to);
    geometry =
      GeometryUtils.makeLineString(
        List.of(new Coordinate(from.getX(), from.getY()), new Coordinate(to.getX(), to.getY()))
      );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("from", fromv).addObj("to", tov).toString();
  }

  @Override
  public State traverse(State s0) {
    StateEditor s1 = createEditorForDrivingOrWalking(s0, this);
    if (s1 == null) {
      return null;
    }

    RoutingRequest options = s0.getOptions();
    s1.incrementWeight(options.elevatorBoardCost);
    s1.incrementTimeInSeconds(options.elevatorBoardTime);

    return s1.makeState();
  }

  @Override
  public I18NString getName() {
    // TODO: i18n
    return new NonLocalizedString("Elevator");
  }

  /**
   * Since board edges always are called Elevator, the name is utterly and completely bogus but is
   * never included in plans.
   */
  @Override
  public boolean hasBogusName() {
    return true;
  }

  @Override
  public LineString getGeometry() {
    return geometry;
  }

  @Override
  public double getDistanceMeters() {
    return 0;
  }
}
