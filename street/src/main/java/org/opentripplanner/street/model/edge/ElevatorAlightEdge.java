package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.LocalizedString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.vertex.ElevatorHopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * A relatively low cost edge for alighting from an elevator. All narrative generation is done by
 * the ElevatorAlightEdge (other edges are silent), because it is the only edge that knows where the
 * user is to get off.
 *
 * @author mattwigway
 */
public class ElevatorAlightEdge extends Edge implements BikeWalkableEdge, ElevatorEdge {

  private static final LocalizedString NAME = new LocalizedString("name.elevator");

  /**
   * The polyline geometry of this edge. It's generally a polyline with two coincident points, but
   * some elevators have horizontal dimension, e.g. the ones on the Eiffel Tower.
   */
  private final LineString the_geom;

  /**
   * @param from the vertex inside the elevator
   * @param to the vertex on the street network
   */
  private ElevatorAlightEdge(ElevatorHopVertex from, Vertex to) {
    super(from, to);
    // set up the geometry
    Coordinate[] coords = new Coordinate[2];
    coords[0] = new Coordinate(from.getX(), from.getY());
    coords[1] = new Coordinate(to.getX(), to.getY());
    the_geom = GeometryUtils.getGeometryFactory().createLineString(coords);
  }

  public static ElevatorAlightEdge createElevatorAlightEdge(ElevatorHopVertex from, Vertex to) {
    return connectToGraph(new ElevatorAlightEdge(from, to));
  }

  @Override
  public State[] traverse(State s0) {
    StateEditor s1 = createEditorForDrivingOrWalking(s0, this);
    s1.incrementWeight(1);
    return s1.makeStateArray();
  }

  @Override
  public I18NString getName() {
    return NAME;
  }

  @Override
  public LineString getGeometry() {
    return the_geom;
  }

  @Override
  public double getDistanceMeters() {
    return 0;
  }
}
