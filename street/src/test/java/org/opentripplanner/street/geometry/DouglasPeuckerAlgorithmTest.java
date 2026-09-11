package org.opentripplanner.street.geometry;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class DouglasPeuckerAlgorithmTest {

  private static final Coordinate BERLIN = new Coordinate(13.4105, 52.5212);
  private static final Coordinate HAMBURG = new Coordinate(10.0003, 53.5566);
  private static final Coordinate HANNOVER = new Coordinate(9.732, 52.376);

  @Test
  void simplifyDisabledWhenToleranceIsZero() {
    var line = GeometryUtils.makeLineString(BERLIN, HAMBURG);
    assertEquals(line, DouglasPeuckerAlgorithm.of(line, 0));
  }

  @Test
  void simplifyRejectsNegativeTolerance() {
    var zigzag = GeometryUtils.makeLineString(BERLIN, HAMBURG);
    assertThrows(IllegalArgumentException.class, () -> DouglasPeuckerAlgorithm.of(zigzag, -5));
  }

  @Test
  void simplifyRejectsDoesNothingForTwoPoints() {
    var line = GeometryUtils.makeLineString(BERLIN, HAMBURG);
    assertThat(DouglasPeuckerAlgorithm.of(line, 5)).isSameInstanceAs(line);
  }

  @Test
  void simplifyReducesPointsButKeepsEndpoints() {
    // A near-straight line with a tiny zigzag - well within a 50 m tolerance, so every
    // interior point should be removable, leaving just the two endpoints.
    var zigzag = GeometryUtils.makeLineString(BERLIN, HANNOVER, HAMBURG);
    var line = DouglasPeuckerAlgorithm.of(zigzag, 50.0);

    assertEquals(3, line.getNumPoints());
    assertEquals(zigzag.getStartPoint().getCoordinate(), line.getStartPoint().getCoordinate());
    assertEquals(zigzag.getEndPoint().getCoordinate(), line.getEndPoint().getCoordinate());

    var simplified = DouglasPeuckerAlgorithm.of(zigzag, 250_000.0);

    assertEquals(2, simplified.getNumPoints());
    assertEquals(
      zigzag.getStartPoint().getCoordinate(),
      simplified.getStartPoint().getCoordinate()
    );
    assertEquals(zigzag.getEndPoint().getCoordinate(), simplified.getEndPoint().getCoordinate());
  }

  /// At 60 degrees latitude a degree of longitude covers about half as many meters as a degree
  /// of latitude. A naive lat-only meters-to-degrees conversion would treat this east-west
  /// zigzag (deliberately deviating in longitude only) as almost twice as far from the line as
  /// it really is, and fail to simplify it despite the real-world deviation being well within
  /// tolerance.
  @Test
  void simplifyAccountsForLongitudeDegreesShrinkingAwayFromEquator() {
    var start = new Coordinate(60.0, 60.0);
    var end = new Coordinate(62.0, 62.0);

    var point = new Coordinate(61.0, 61.001);
    var LINE = GeometryUtils.makeLineString(start, point, end);
    assertThat(DouglasPeuckerAlgorithm.of(LINE, 48.50).getCoordinates())
      .asList()
      .containsExactly(start, point, end);
    assertThat(DouglasPeuckerAlgorithm.of(LINE, 48.51).getCoordinates())
      .asList()
      .containsExactly(start, end);

    point = new Coordinate(61.001, 61.0);
    LINE = GeometryUtils.makeLineString(start, point, end);
    assertThat(DouglasPeuckerAlgorithm.of(LINE, 48.50).getCoordinates())
      .asList()
      .containsExactly(start, point, end);
    assertThat(DouglasPeuckerAlgorithm.of(LINE, 48.51).getCoordinates())
      .asList()
      .containsExactly(start, end);
  }
}
