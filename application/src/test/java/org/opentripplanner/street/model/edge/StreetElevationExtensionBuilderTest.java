package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;

class StreetElevationExtensionBuilderTest {

  private static final Coordinate[] COORDINATES_ONE_POINT = new Coordinate[] {
    new Coordinate(0, 0),
  };
  private static final PackedCoordinateSequence ELEVATION_PROFILE_ONE_POINT =
    new PackedCoordinateSequence.Double(COORDINATES_ONE_POINT, 2);

  private static final Coordinate[] COORDINATES_TWO_POINTS = new Coordinate[] {
    new Coordinate(0, 0),
    new Coordinate(1, 1),
  };
  private static final PackedCoordinateSequence ELEVATION_PROFILE_TWO_POINTS =
    new PackedCoordinateSequence.Double(COORDINATES_TWO_POINTS, 2);

  private static final LineString GEOMETRY = GeometryUtils.getGeometryFactory()
    .createLineString(
      new Coordinate[] {
        StreetModelForTest.V1.getCoordinate(),
        StreetModelForTest.V2.getCoordinate(),
      }
    );
  private StreetEdgeBuilder<?> streetEdgeBuilder;

  @BeforeEach
  void setup() {
    streetEdgeBuilder = new StreetEdgeBuilder<>()
      .withPermission(StreetTraversalPermission.ALL)
      .withFromVertex(StreetModelForTest.V1)
      .withToVertex(StreetModelForTest.V2)
      .withGeometry(GEOMETRY);
  }

  @Test
  void testInvalidElevationProfile() {
    StreetElevationExtensionBuilder seeb = new StreetElevationExtensionBuilder()
      .withPermission(StreetTraversalPermission.ALL)
      .withDistanceInMeters(1)
      .withElevationProfile(ELEVATION_PROFILE_ONE_POINT);
    assertTrue(seeb.build().isEmpty());
  }

  @Test
  void testValidElevationProfile() {
    StreetElevationExtensionBuilder seeb = new StreetElevationExtensionBuilder()
      .withPermission(StreetTraversalPermission.ALL)
      .withDistanceInMeters(1)
      .withElevationProfile(ELEVATION_PROFILE_TWO_POINTS);
    Optional<StreetElevationExtension> streetElevationExtension = seeb.build();
    assertFalse(streetElevationExtension.isEmpty());
  }

  @Test
  void testBuildFromStreetEdge() {
    StreetEdge se = streetEdgeBuilder.buildAndConnect();
    StreetElevationExtensionBuilder seeb = StreetElevationExtensionBuilder.of(
      se
    ).withElevationProfile(ELEVATION_PROFILE_TWO_POINTS);
    Optional<StreetElevationExtension> streetElevationExtension = seeb.build();
    assertFalse(streetElevationExtension.isEmpty());
  }

  @Test
  void testBuildFromStreetEdgeBuilder() {
    StreetElevationExtensionBuilder seebFromStreetEdgeBuilder = StreetElevationExtensionBuilder.of(
      streetEdgeBuilder
    )
      .withElevationProfile(ELEVATION_PROFILE_TWO_POINTS)
      .withDistanceInMeters(1);
    Optional<StreetElevationExtension> streetElevationExtensionFromStreetEdgeBuilder =
      seebFromStreetEdgeBuilder.build();
    assertFalse(streetElevationExtensionFromStreetEdgeBuilder.isEmpty());

    StreetElevationExtensionBuilder seebFromStreetEdge = StreetElevationExtensionBuilder.of(
      streetEdgeBuilder.buildAndConnect()
    )
      .withElevationProfile(ELEVATION_PROFILE_TWO_POINTS)
      .withDistanceInMeters(1);
    Optional<StreetElevationExtension> streetElevationExtensionFromStreetEdge =
      seebFromStreetEdge.build();

    assertEquals(
      streetElevationExtensionFromStreetEdge.orElseThrow().toString(),
      streetElevationExtensionFromStreetEdge.get().toString(),
      "Street elevation profiles built from StreetEdge and from StreetEdgeBuilder should be identical"
    );
  }
}
