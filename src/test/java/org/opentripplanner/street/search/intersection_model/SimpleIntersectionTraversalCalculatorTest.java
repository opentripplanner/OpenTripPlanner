package org.opentripplanner.street.search.intersection_model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;

/**
 * Tests for SimpleIntersectionTraversalModel.
 * <p>
 * TODO(flamholz): add tests for traversal with traffic lights and without a car.
 *
 * @author avi
 */
public class SimpleIntersectionTraversalCalculatorTest {

  private Graph graph;

  public SimpleIntersectionTraversalCalculator calculator;

  @BeforeEach
  public void before() {
    graph = new Graph();
    calculator = new SimpleIntersectionTraversalCalculator(DrivingDirection.RIGHT);
  }

  @Test
  public void testCalculateTurnAngle() {
    // Graph for a fictional grid city with turn restrictions
    IntersectionVertex v1 = vertex("maple_1st", new Coordinate(2.0, 2.0), false);
    IntersectionVertex v2 = vertex("maple_2nd", new Coordinate(2.0, 1.0), false);
    v2.trafficLight = true;

    StreetEdge e1 = edge(v1, v2, 1.0, false);

    // Edge has same first and last angle.
    assertEquals(90, e1.getInAngle());
    assertEquals(90, e1.getOutAngle());

    // 2 new ones
    IntersectionVertex v3 = vertex("test2", new Coordinate(1.0, 1.0), false);

    // Second edge
    StreetEdge e2 = edge(v2, v3, 1.0, false);

    assertEquals(0, e2.getInAngle());
    assertEquals(0, e2.getOutAngle());

    // Difference should be about 90.
    int diff = (e1.getOutAngle() - e2.getInAngle());
    assertEquals(90, diff);

    // calculate the angle for driving on the right hand side

    int rightHandDriveAngle = calculator.calculateTurnAngle(e1, e2);
    assertEquals(270, rightHandDriveAngle);
    assertTrue(calculator.isTurnAcrossTraffic(rightHandDriveAngle));
    assertFalse(calculator.isSafeTurn(rightHandDriveAngle));

    // and on the left hand side

    var leftHandDriveCostModel = new SimpleIntersectionTraversalCalculator(DrivingDirection.LEFT);
    int leftHandDriveAngle = leftHandDriveCostModel.calculateTurnAngle(e1, e2);
    assertEquals(270, leftHandDriveAngle);

    assertTrue(leftHandDriveCostModel.isSafeTurn(leftHandDriveAngle));
    assertFalse(leftHandDriveCostModel.isTurnAcrossTraffic(leftHandDriveAngle));

    // on a bike the turn cost for crossing traffic (left turn in left hand driving countries)
    // should be higher than going the opposite direction

    assertEquals(
      1.6875,
      calculator.computeTraversalDuration(v2, e1, e2, TraverseMode.BICYCLE, 40, 40),
      0.1
    );
    assertEquals(
      0.5625,
      calculator.computeTraversalDuration(v2, e2, e1, TraverseMode.BICYCLE, 40, 40),
      0.1
    );

    // in left hand driving countries it should be the opposite

    assertEquals(
      0.5625,
      leftHandDriveCostModel.computeTraversalDuration(v2, e1, e2, TraverseMode.BICYCLE, 40, 40),
      0.1
    );
    assertEquals(
      1.6875,
      leftHandDriveCostModel.computeTraversalDuration(v2, e2, e1, TraverseMode.BICYCLE, 40, 40),
      0.1
    );
  }

  @Test
  public void testTurnDirectionChecking() {
    // 3 points on a roughly on line
    Coordinate a = new Coordinate(-73.990989, 40.750167);
    Coordinate b = new Coordinate(-73.988049, 40.749094);
    Coordinate c = new Coordinate(-73.984981, 40.747761);

    // A vertex for each. No light.
    IntersectionVertex u = vertex("from_v", a, false);
    IntersectionVertex v = vertex("intersection", b, false);
    IntersectionVertex w = vertex("to_v", c, false);

    // Two edges.
    StreetEdge fromEdge = edge(u, v, 1.0, false);
    StreetEdge toEdge = edge(v, w, 1.0, false);

    int turnAngle = calculator.calculateTurnAngle(fromEdge, toEdge);
    assertFalse(calculator.isSafeTurn(turnAngle));
    assertFalse(calculator.isTurnAcrossTraffic(turnAngle));
    // AKA is a straight ahead.
  }

  @Test
  public void testFreeFlowing() {
    // 3 points on a roughly on line
    Coordinate a = new Coordinate(-73.990989, 40.750167);
    Coordinate b = new Coordinate(-73.988049, 40.749094);
    Coordinate c = new Coordinate(-73.984981, 40.747761);

    // A vertex for each. No light.
    IntersectionVertex u = vertex("from_v", a, false);
    IntersectionVertex v = vertex("intersection", b, false);
    IntersectionVertex w = vertex("to_v", c, false);
    v.freeFlowing = true;

    // Two edges.
    StreetEdge fromEdge = edge(u, v, 1.0, false);
    StreetEdge toEdge = edge(v, w, 1.0, false);

    float fromSpeed = 1.0f;
    float toSpeed = 1.0f;
    TraverseMode mode = TraverseMode.CAR;

    double traversalCost = calculator.computeTraversalDuration(
      v,
      fromEdge,
      toEdge,
      mode,
      fromSpeed,
      toSpeed
    );

    // Vertex is free-flowing so cost should be 0.0.
    assertEquals(0.0, traversalCost, 0.0);
  }

  @Test
  public void testInferredFreeFlowing() {
    // 3 points on a roughly on line
    Coordinate a = new Coordinate(-73.990989, 40.750167);
    Coordinate b = new Coordinate(-73.988049, 40.749094);
    Coordinate c = new Coordinate(-73.984981, 40.747761);

    // A vertex for each. No light.
    IntersectionVertex u = vertex("from_v", a, false);
    IntersectionVertex v = vertex("intersection", b, false);
    IntersectionVertex w = vertex("to_v", c, false);

    // Two edges - will infer that the vertex is free-flowing since there is no light.
    StreetEdge fromEdge = edge(u, v, 1.0, false);
    StreetEdge toEdge = edge(v, w, 1.0, false);

    float fromSpeed = 1.0f;
    float toSpeed = 1.0f;
    TraverseMode mode = TraverseMode.CAR;

    double traversalCost = calculator.computeTraversalDuration(
      v,
      fromEdge,
      toEdge,
      mode,
      fromSpeed,
      toSpeed
    );

    // Vertex is free-flowing so cost should be 0.0.
    assertEquals(0.0, traversalCost, 0.0);
  }

  @Test
  public void testStraightNoLightInCar() {
    // 3 points on a roughly on line
    Coordinate a = new Coordinate(-73.990989, 40.750167);
    Coordinate b = new Coordinate(-73.988049, 40.749094);
    Coordinate c = new Coordinate(-73.984981, 40.747761);

    // A vertex for each. No light.
    IntersectionVertex u = vertex("from_v", a, false);
    IntersectionVertex v = vertex("intersection", b, false);
    IntersectionVertex w = vertex("to_v", c, false);

    // Two edges.
    StreetEdge fromEdge = edge(u, v, 1.0, false);
    StreetEdge toEdge = edge(v, w, 1.0, false);

    // 3rd edge prevents inferral of free-flowingness
    StreetEdge extraEdge = edge(v, u, 1.0, false);

    float fromSpeed = 1.0f;
    float toSpeed = 1.0f;
    TraverseMode mode = TraverseMode.CAR;

    double traversalCost = calculator.computeTraversalDuration(
      v,
      fromEdge,
      toEdge,
      mode,
      fromSpeed,
      toSpeed
    );

    // Cost with default values = 5.0
    assertEquals(5, traversalCost, 0.0);
  }

  @Test
  public void testRightNoLightInCar() {
    // 3 points that form a right turn on the map
    Coordinate a = new Coordinate(40.750167, -73.990989);
    Coordinate b = new Coordinate(40.749094, -73.988049);
    Coordinate c = new Coordinate(40.748509, -73.988693);

    // A vertex for each. No light.
    IntersectionVertex u = vertex("from_v", a, false);
    IntersectionVertex v = vertex("intersection", b, false);
    IntersectionVertex w = vertex("to_v", c, false);

    // Two edges.
    StreetEdge fromEdge = edge(u, v, 1.0, false);
    StreetEdge toEdge = edge(v, w, 1.0, false);

    // 3rd edge prevents inferral of free-flowingness
    StreetEdge extraEdge = edge(v, u, 1.0, false);

    int turnAngle = calculator.calculateTurnAngle(fromEdge, toEdge);
    assertTrue(calculator.isSafeTurn(turnAngle));
    assertFalse(calculator.isTurnAcrossTraffic(turnAngle));

    float fromSpeed = 1.0f;
    float toSpeed = 1.0f;
    TraverseMode mode = TraverseMode.CAR;

    double traversalCost = calculator.computeTraversalDuration(
      v,
      fromEdge,
      toEdge,
      mode,
      fromSpeed,
      toSpeed
    );

    // Cost with default values = 8.0
    assertEquals(8.0, traversalCost, 0.0);
  }

  @Test
  public void testLeftNoLightInCar() {
    // 3 points that form a right turn on the map
    Coordinate a = new Coordinate(40.750167, -73.990989);
    Coordinate b = new Coordinate(40.749094, -73.988049);
    Coordinate c = new Coordinate(40.749760, -73.987749);

    // A vertex for each. No light.
    IntersectionVertex u = vertex("from_v", a, false);
    IntersectionVertex v = vertex("intersection", b, false);
    IntersectionVertex w = vertex("to_v", c, false);

    // Two edges.
    StreetEdge fromEdge = edge(u, v, 1.0, false);
    StreetEdge toEdge = edge(v, w, 1.0, false);

    // 3rd edge prevents inferral of free-flowingness
    StreetEdge extraEdge = edge(v, u, 1.0, false);

    int turnAngle = calculator.calculateTurnAngle(fromEdge, toEdge);
    assertFalse(calculator.isSafeTurn(turnAngle));
    assertTrue(calculator.isTurnAcrossTraffic(turnAngle));

    float fromSpeed = 1.0f;
    float toSpeed = 1.0f;
    TraverseMode mode = TraverseMode.CAR;

    double traversalCost = calculator.computeTraversalDuration(
      v,
      fromEdge,
      toEdge,
      mode,
      fromSpeed,
      toSpeed
    );

    // Cost with default values = 8.0
    assertEquals(8.0, traversalCost, 0.0);
  }

  /****
   * Private Methods
   ****/

  private IntersectionVertex vertex(String label, Coordinate coord, boolean hasLight) {
    IntersectionVertex v = new IntersectionVertex(graph, label, coord.y, coord.x);
    v.trafficLight = hasLight;
    return v;
  }

  /**
   * Create an edge. If twoWay, create two edges (back and forth).
   *
   * @param back true if this is a reverse edge
   */
  private StreetEdge edge(StreetVertex vA, StreetVertex vB, double length, boolean back) {
    String labelA = vA.getLabel();
    String labelB = vB.getLabel();
    String name = String.format("%s_%s", labelA, labelB);
    Coordinate[] coords = new Coordinate[2];
    coords[0] = vA.getCoordinate();
    coords[1] = vB.getCoordinate();
    LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

    StreetTraversalPermission perm = StreetTraversalPermission.ALL;
    return new StreetEdge(vA, vB, geom, name, length, perm, back);
  }
}
