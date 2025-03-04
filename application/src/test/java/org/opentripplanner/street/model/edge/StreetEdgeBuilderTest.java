package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.StreetVertex;

class StreetEdgeBuilderTest {

  private static final StreetVertex FROM_VERTEX = StreetModelForTest.V1;
  private static final StreetVertex TO_VERTEX = StreetModelForTest.V2;
  private static final StreetTraversalPermission STREET_TRAVERSAL_PERMISSION =
    StreetTraversalPermission.ALL;

  private static final I18NString NAME = I18NString.of("street-edge-name");

  private static final double LENGTH = 10.5;
  private static final LineString GEOMETRY = GeometryUtils.getGeometryFactory()
    .createLineString(new Coordinate[] { FROM_VERTEX.getCoordinate(), TO_VERTEX.getCoordinate() });

  private static final boolean WHEELCHAIR_ACCESSIBLE = false;
  private static final boolean BACK = false;
  private static final boolean STAIRS = true;
  private static final float CAR_SPEED = 100.1f;
  private static final float WALK_SAFETY_FACTOR = 0.5f;
  private static final float BICYCLE_SAFETY_FACTOR = 0.4f;
  private static final boolean SLOPE_OVERRIDE = false;
  private static final boolean NAME_IS_DERIVED = true;
  private static final boolean BICYCLE_NO_THRU_TRAFFIC = true;
  private static final boolean MOTOR_VEHICLE_NO_THRU_TRAFFIC = true;
  private static final boolean WALK_NO_THRU_TRAFFIC = true;
  private static final I18NString NEW_NAME = I18NString.of("street-edge-new-name");

  @Test
  void buildWithDefaultLength() {
    StreetEdge streetEdge = new StreetEdgeBuilder<>()
      .withFromVertex(FROM_VERTEX)
      .withToVertex(TO_VERTEX)
      .withGeometry(GEOMETRY)
      .withName(NAME)
      .withPermission(STREET_TRAVERSAL_PERMISSION)
      .buildAndConnect();
    assertEquals(
      StreetEdge.defaultMillimeterLength(GEOMETRY) / 1000.0,
      streetEdge.getDistanceMeters()
    );
  }

  @Test
  void buildWithCustomLength() {
    StreetEdge streetEdge = buildStreetEdge();
    assertEquals(NAME, streetEdge.getName());
    assertAllProperties(streetEdge);
  }

  @Test
  void buildFromOriginal() {
    StreetEdge original = buildStreetEdge();
    StreetEdge streetEdge = new StreetEdgeBuilder<>(original).withName(NEW_NAME).buildAndConnect();
    assertEquals(NEW_NAME, streetEdge.getName());
    assertAllProperties(streetEdge);
  }

  private static StreetEdge buildStreetEdge() {
    return new StreetEdgeBuilder<>()
      .withFromVertex(FROM_VERTEX)
      .withToVertex(TO_VERTEX)
      .withMeterLength(LENGTH)
      .withName(NAME)
      .withPermission(STREET_TRAVERSAL_PERMISSION)
      .withCarSpeed(CAR_SPEED)
      .withWalkSafetyFactor(WALK_SAFETY_FACTOR)
      .withBicycleSafetyFactor(BICYCLE_SAFETY_FACTOR)
      .withWheelchairAccessible(WHEELCHAIR_ACCESSIBLE)
      .withBack(BACK)
      .withStairs(STAIRS)
      .withSlopeOverride(SLOPE_OVERRIDE)
      .withBogusName(NAME_IS_DERIVED)
      .withWalkNoThruTraffic(WALK_NO_THRU_TRAFFIC)
      .withBicycleNoThruTraffic(BICYCLE_NO_THRU_TRAFFIC)
      .withMotorVehicleNoThruTraffic(MOTOR_VEHICLE_NO_THRU_TRAFFIC)
      .buildAndConnect();
  }

  private static void assertAllProperties(StreetEdge streetEdge) {
    assertEquals(FROM_VERTEX, streetEdge.getFromVertex());
    assertEquals(TO_VERTEX, streetEdge.getToVertex());
    assertEquals(LENGTH, streetEdge.getDistanceMeters());
    assertEquals(STREET_TRAVERSAL_PERMISSION, streetEdge.getPermission());
    assertEquals(WHEELCHAIR_ACCESSIBLE, streetEdge.isWheelchairAccessible());
    assertEquals(STAIRS, streetEdge.isStairs());
    assertEquals(CAR_SPEED, streetEdge.getCarSpeed());
    assertEquals(WALK_SAFETY_FACTOR, streetEdge.getWalkSafetyFactor());
    assertEquals(BICYCLE_SAFETY_FACTOR, streetEdge.getBicycleSafetyFactor());
    assertEquals(SLOPE_OVERRIDE, streetEdge.isSlopeOverride());
    assertEquals(NAME_IS_DERIVED, streetEdge.nameIsDerived());
    assertEquals(WALK_NO_THRU_TRAFFIC, streetEdge.isWalkNoThruTraffic());
    assertEquals(BICYCLE_NO_THRU_TRAFFIC, streetEdge.isBicycleNoThruTraffic());
    assertEquals(MOTOR_VEHICLE_NO_THRU_TRAFFIC, streetEdge.isMotorVehicleNoThruTraffic());
  }
}
