package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import net.opengis.gml._3.PointPropertyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;

class LineStringMapperTest {

  // (latitude, longitude) pairs, as NeTEx orders them
  private static final Double[] COORDINATES = {
    59.90929,
    10.74527,
    59.90893,
    10.74493,
    59.90870,
    10.74585,
  };

  public static final double FLOATING_POINT_COMPARISON_PRECISION = 0.000001;
  public static final String ID = "RUT:ServiceLink:1";

  private DataImportIssueStore issueStore;
  private LineStringMapper lineStringMapper;

  @BeforeEach
  void setUp() {
    issueStore = new DefaultDataImportIssueStore();
    lineStringMapper = new LineStringMapper(issueStore);
  }

  @Test
  void posListWithValidCoordinates() {
    var lineString = lineStringFromPosList(COORDINATES);

    var geometry = lineStringMapper.mapLineString(lineString, ID);

    assertEquals(0, issueStore.listIssues().size());
    assertCoordinatesMatch(COORDINATES, geometry);
  }

  @Test
  void posListWithTooFewCoordinates() {
    var lineString = lineStringFromPosList(COORDINATES[0], COORDINATES[1]);

    var geometry = lineStringMapper.mapLineString(lineString, ID);

    assertNull(geometry);
    assertEquals(1, issueStore.listIssues().size());
  }

  @Test
  void posListWithOddNumberOfCoordinates() {
    var lineString = lineStringFromPosList(COORDINATES[0], COORDINATES[1], COORDINATES[2]);

    var geometry = lineStringMapper.mapLineString(lineString, ID);

    assertNull(geometry);
    assertEquals(1, issueStore.listIssues().size());
  }

  @Test
  void posOrPointPropertyWithValidCoordinates() {
    var lineString = lineStringFromPosOrPointProperty(COORDINATES);

    var geometry = lineStringMapper.mapLineString(lineString, ID);

    assertEquals(0, issueStore.listIssues().size());
    assertCoordinatesMatch(COORDINATES, geometry);
  }

  @Test
  void posOrPointPropertyWithTooFewCoordinates() {
    var lineString = lineStringFromPosOrPointProperty(COORDINATES[0], COORDINATES[1]);

    var geometry = lineStringMapper.mapLineString(lineString, ID);

    assertNull(geometry);
    assertEquals(1, issueStore.listIssues().size());
  }

  @Test
  void posOrPointPropertyWithBadElementType() {
    var posOrPoints = new ArrayList<>();
    posOrPoints.add(new DirectPositionType().withValue(COORDINATES[0], COORDINATES[1]));
    // a PointPropertyType is a legal member of the posOrPointProperty group in the schema, but
    // this mapper only understands DirectPositionType elements.
    posOrPoints.add(new PointPropertyType());
    posOrPoints.add(new DirectPositionType().withValue(COORDINATES[2], COORDINATES[3]));
    var lineString = new LineStringType().withPosOrPointProperty(posOrPoints);

    var geometry = lineStringMapper.mapLineString(lineString, ID);

    assertNotNull(geometry);
    assertEquals(1, issueStore.listIssues().size());
    assertEquals("BadLineStringElementType", issueStore.listIssues().get(0).getType());
    assertCoordinatesMatch(
      new Double[] { COORDINATES[0], COORDINATES[1], COORDINATES[2], COORDINATES[3] },
      geometry
    );
  }

  @Test
  void zeroLengthGeometryIsRejected() {
    // both points are identical, so the resulting linestring has zero length
    var lineString = lineStringFromPosList(
      COORDINATES[0],
      COORDINATES[1],
      COORDINATES[0],
      COORDINATES[1]
    );

    var geometry = lineStringMapper.mapLineString(lineString, ID);

    assertNull(geometry);
    assertEquals(1, issueStore.listIssues().size());
    assertEquals("ServiceLinkGeometryError", issueStore.listIssues().get(0).getType());
  }

  @Test
  void neitherPosListNorPosOrPointProperty() {
    var lineString = new LineStringType();

    var geometry = lineStringMapper.mapLineString(lineString, ID);

    assertNull(geometry);
    assertEquals(1, issueStore.listIssues().size());
  }

  private static LineStringType lineStringFromPosList(Double... coordinates) {
    return new LineStringType().withPosList(new DirectPositionListType().withValue(coordinates));
  }

  private static LineStringType lineStringFromPosOrPointProperty(Double... coordinates) {
    var posOrPoints = new ArrayList<>();
    for (int i = 0; i < coordinates.length; i += 2) {
      posOrPoints.add(new DirectPositionType().withValue(coordinates[i], coordinates[i + 1]));
    }
    return new LineStringType().withPosOrPointProperty(posOrPoints);
  }

  /** {@code coordinates} are (latitude, longitude) pairs, as NeTEx orders them. */
  private static void assertCoordinatesMatch(Double[] coordinates, LineString geometry) {
    assertNotNull(geometry);
    Coordinate[] actual = geometry.getCoordinates();
    assertEquals(coordinates.length / 2, actual.length);
    for (int i = 0; i < actual.length; i++) {
      assertEquals(coordinates[i * 2], actual[i].getY(), FLOATING_POINT_COMPARISON_PRECISION);
      assertEquals(coordinates[i * 2 + 1], actual[i].getX(), FLOATING_POINT_COMPARISON_PRECISION);
    }
  }
}
