package org.opentripplanner.street.geometry;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.geometry.DouglasPeuckerAlgorithm.perpendicularDistance;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;

class DouglasPeuckerAlgorithmTest {

  private static final String SP = "\\s+";
  private static final String NUM = "(-?\\d+(?:\\.\\d+)?)";
  private static final String COOR = "\\( ?" + NUM + " +" + NUM + "\\)";
  private static final Pattern PERPENDICULAR_DISTANCE_PTN = Pattern.compile(
    COOR + SP + COOR + SP + COOR + SP + NUM + SP + "=" + SP + NUM + ".*"
  );

  private static final Coordinate BERLIN = new WgsCoordinate(52.5212, 13.4105).asJtsCoordinate();
  private static final Coordinate HAMBURG = new WgsCoordinate(53.5566, 10.0003).asJtsCoordinate();
  private static final Coordinate HANNOVER = new WgsCoordinate(52.376, 9.732).asJtsCoordinate();

  // 0.0000001 degrees is ~1 centimeter at the equator
  private static final double ON_CENTI_METER_DEGREES = 0.0000001;

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
    // Hannover is ~125 km off the direct Berlin-Hamburg line, so it should survive a
    // 120 km tolerance; only a larger 130 km tolerance removes it.
    var route = GeometryUtils.makeLineString(BERLIN, HANNOVER, HAMBURG);
    var line = DouglasPeuckerAlgorithm.of(route, 120_000.0);

    assertEquals(3, line.getNumPoints());
    assertEquals(route.getStartPoint().getCoordinate(), line.getStartPoint().getCoordinate());
    assertEquals(route.getEndPoint().getCoordinate(), line.getEndPoint().getCoordinate());

    var simplified = DouglasPeuckerAlgorithm.of(route, 130_000.0);

    assertEquals(2, simplified.getNumPoints());
    assertEquals(route.getStartPoint().getCoordinate(), simplified.getStartPoint().getCoordinate());
    assertEquals(route.getEndPoint().getCoordinate(), simplified.getEndPoint().getCoordinate());
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

  /// A spike that overshoots past `end` while staying on (near) the same bearing as the
  /// start-end chord is the classic Douglas-Peucker failure mode: measured against the
  /// *infinite line* through start/end, the spike looks colinear (~0 distance) no matter how
  /// far out it goes, so a naive implementation erases it at any tolerance. Measuring against
  /// the *segment* instead - clamping the projection to [start, end] - fixes this: the spike's
  /// distance becomes the real distance to the nearest endpoint, so it survives.
  @Test
  void simplifyKeepsASpikeThatOvershootsPastTheChordEndpoint() {
    var start = new WgsCoordinate(60.0, 10.0);
    var end = start.moveEastMeters(1000);
    var spikeTip = start.moveEastMeters(3000);

    var line = GeometryUtils.makeLineString(
      start.asJtsCoordinate(),
      spikeTip.asJtsCoordinate(),
      end.asJtsCoordinate()
    );
    var simplified = DouglasPeuckerAlgorithm.of(line, 5.0);

    assertThat(simplified.getCoordinates())
      .asList()
      .containsExactly(start.asJtsCoordinate(), spikeTip.asJtsCoordinate(), end.asJtsCoordinate());
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      "(60 10) (61 20) (60 30) 1.0 = 1.0  --  Ɛ = 1º latitude",
      "(-9  0) ( 0  1) ( 9  0) 1.0 = 1.0  --  Ɛ = 1º longitude at equator",
      "(57  0) (60  2) (63  0) 0.5 = 1.0  --  Ɛ = 1º longitude at 60º North",
      "(-59 0) (-60 2) (-61 0) 0.5 = 1.0  --  Ɛ = 1º longitude at 60º South",
      "( 0 10) ( 0 22) ( 0 20) 1.0 = 2.0  --  Ɛ = 2º overshootingting the end point (longitude)",
      "( 0 55) (-5 55) (30 55) 1.0 = 5.0  --  Ɛ = 5º degrees overshootingting the start point (latitude)",
      "( 0 50) (-4 47) (0  80) 1.0 = 5.0  --  Ɛ = 4º x 3º = 5º overshootingting the start point",
      "(58 10) (65 18) (62 10) 0.5 = 5.0  --  Ɛ = 3º x 8º = 5º overshootingting the end point at 60º North",
    }
  )
  void testPerpendicularDistance(String text) {
    var m = PERPENDICULAR_DISTANCE_PTN.matcher(text);
    assertTrue(m.matches(), text);

    var start = new WgsCoordinate(num(m, 1), num(m, 2)).asJtsCoordinate();
    var point = new WgsCoordinate(num(m, 3), num(m, 4)).asJtsCoordinate();
    var end = new WgsCoordinate(num(m, 5), num(m, 6)).asJtsCoordinate();
    double lonScale = num(m, 7);
    double expected = num(m, 8);

    assertEquals(
      expected,
      perpendicularDistance(start, end, point, lonScale),
      ON_CENTI_METER_DEGREES,
      text
    );
  }

  private static double num(Matcher m, int group) {
    return Double.parseDouble(m.group(group));
  }
}
