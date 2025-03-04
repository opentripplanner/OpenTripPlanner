package org.opentripplanner.service.worldenvelope.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.geometry.WgsCoordinate;

class WorldEnvelopeTest {

  private static final int S10 = -10;
  private static final int S20 = -20;
  private static final int N30 = 30;
  private static final int N40 = 40;
  private static final int E50 = 50;
  private static final int E160 = 160;
  private static final int W60 = -60;
  private static final int W170 = -170;

  /**
   * To make sure we cover all cases we add a case for each combination of:
   *  - latitude
   *    - south hemisphere
   *    - north hemisphere
   *    - both sides of the equator
   *  - longitude
   *    - east side of 0º (Greenwich)
   *    - west side of 0º
   *    - both sides of 0º
   *    - both sides of 180º
   * Skip cases for North- and South-pole - not relevant - obscure cases)
   */
  static List<Arguments> testCases() {
    return List.of(
      // name, lower-lat, left-lon, upper-lat, right-lon, center-lat, center-lon
      Arguments.of("South-East", S20, E50, S10, E160, -15d, 105d),
      Arguments.of("Equator-East", S10, E50, N30, E160, 10d, 105d),
      Arguments.of("North-East", N30, E50, N40, E160, 35d, 105d),
      Arguments.of("South-West", S20, W170, S10, W60, -15d, -115d),
      Arguments.of("Equator-West", S10, W170, N30, W60, 10d, -115d),
      Arguments.of("North-West", N30, W170, N40, W60, 35d, -115d),
      Arguments.of("North-Greenwich", N30, W60, N40, E50, 35d, -5d),
      Arguments.of("Equator-Greenwich", S10, W60, N30, E50, 10d, -5d),
      Arguments.of("South-Greenwich", S20, W60, S10, E50, -15d, -5d),
      Arguments.of("North-180º", N30, E160, N40, W170, 35d, 175d),
      Arguments.of("Equator-180º", S10, E160, N30, W170, 10d, 175d),
      Arguments.of("South-180º", S20, E160, S10, W170, -15d, 175d)
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testWorldEnvelope(
    String name,
    double lowerLat,
    double leftLon,
    double upperLat,
    double rightLon,
    double centerLat,
    double centerLon
  ) {
    // Add a point close to the center
    var median = new WgsCoordinate(centerLat + 1.0, centerLon + 1.0);

    // WorldEnvelope should normalize to lower-left and upper-right
    // Add lower-right & upper-left the world-envelope
    var subjectWithoutMedian = WorldEnvelope.of()
      .expandToIncludeStreetEntities(lowerLat, rightLon)
      .expandToIncludeStreetEntities(upperLat, leftLon)
      .build();
    // Add the ~middle point between each corner of the envelope + median point
    // We offset the one center value to the "other" side of the median by adding 2.0
    var subjectWithMedian = WorldEnvelope.of()
      .expandToIncludeTransitEntities(
        List.of(
          new WgsCoordinate(upperLat, centerLon),
          new WgsCoordinate(lowerLat, centerLon + 2d),
          new WgsCoordinate(centerLat, rightLon),
          new WgsCoordinate(centerLat + 2d, leftLon),
          median
        ),
        WgsCoordinate::latitude,
        WgsCoordinate::longitude
      )
      .build();

    for (WorldEnvelope subject : List.of(subjectWithoutMedian, subjectWithMedian)) {
      assertEquals(lowerLat, subject.lowerLeft().latitude(), name + " lower-latitude");
      assertEquals(leftLon, subject.lowerLeft().longitude(), name + " left-longitude");
      assertEquals(upperLat, subject.upperRight().latitude(), name + " upper-latitude");
      assertEquals(rightLon, subject.upperRight().longitude(), name + " right-longitude");
      assertEquals(centerLat, subject.meanCenter().latitude(), name + " center-latitude");
      assertEquals(centerLon, subject.meanCenter().longitude(), name + " center-longitude");
    }

    assertTrue(
      subjectWithoutMedian.medianCenter().isEmpty(),
      "First envelope does not have a median"
    );
    assertTrue(subjectWithMedian.medianCenter().isPresent(), "Second envelope does have a median");
    assertEquals(median, subjectWithMedian.medianCenter().get(), name + " median");
  }

  @Test
  void testWorldEnvelopeToString() {
    assertEquals(
      "WorldEnvelope{lowerLeft: (-10.0, -60.0), upperRight: (40.0, 50.0), meanCenter: (15.0, -5.0)}",
      WorldEnvelope.of()
        .expandToIncludeStreetEntities(S10, E50)
        .expandToIncludeStreetEntities(N40, W60)
        .build()
        .toString()
    );
  }
}
