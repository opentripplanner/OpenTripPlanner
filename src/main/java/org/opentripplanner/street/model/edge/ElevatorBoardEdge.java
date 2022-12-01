package org.opentripplanner.street.model.edge;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.street.model.vertex.ElevatorOffboardVertex;
import org.opentripplanner.street.model.vertex.ElevatorOnboardVertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.NonLocalizedString;

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

    var streetPreferences = s0.getPreferences().street();

    s1.incrementWeight(streetPreferences.elevator().boardCost());
    s1.incrementTimeInSeconds(streetPreferences.elevator().boardTime());

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
