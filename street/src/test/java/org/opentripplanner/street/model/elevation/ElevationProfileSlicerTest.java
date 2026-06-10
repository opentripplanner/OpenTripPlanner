package org.opentripplanner.street.model.elevation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;

class ElevationProfileSlicerTest {

  @Test
  void slice() {
    double[] two_point = new double[] { 0, 10, 10, 20 };
    double[] four_point = new double[] { 0, 100, 10, 110, 20, 120, 25, 125 };
    double[] small_run = new double[] { 0, 100, 10, 110, 20, 120, 20.5, 120.5 };

    // Full elevation is returned
    assertPartialElevation(two_point, 0, 10, two_point);
    assertPartialElevation(two_point, -10, 20, two_point);
    assertPartialElevation(four_point, 0, 25, four_point);
    assertPartialElevation(four_point, -10, 30, four_point);

    // null is returned for single-point sections
    assertPartialElevation(two_point, 0, 0, null);
    assertPartialElevation(two_point, 10, 10, null);

    // partial sections are returned (from 1 segment)
    assertPartialElevation(two_point, 0, 0.5, new double[] { 0, 10, 0.5, 10.5 });
    assertPartialElevation(two_point, 9.5, 10, new double[] { 0, 19.5, 0.5, 20 });
    assertPartialElevation(two_point, 4, 8, new double[] { 0, 14, 4, 18 });

    // partial sections are returned (along segments)
    assertPartialElevation(four_point, 0, 20, new double[] { 0, 100, 10, 110, 20, 120 });
    assertPartialElevation(four_point, 10, 20, new double[] { 0, 110, 10, 120 });
    assertPartialElevation(four_point, 20, 25, new double[] { 0, 120, 5, 125 });

    // partial sections are returned (between segments)
    assertPartialElevation(four_point, 5, 20, new double[] { 0, 105, 5, 110, 15, 120 });
    assertPartialElevation(four_point, 5, 25, new double[] { 0, 105, 5, 110, 15, 120, 20, 125 });

    assertPartialElevation(small_run, 10, 20.5, new double[] { 0, 110, 10, 120, 10.5, 120.5 });
    assertPartialElevation(
      small_run,
      0,
      20.25,
      new double[] { 0, 100, 10, 110, 20, 120, 20.25, 120.25 }
    );
    assertPartialElevation(
      small_run,
      0.25,
      20.25,
      new double[] { 0, 100.25, 9.75, 110, 19.75, 120, 20, 120.25 }
    );
  }

  private static void assertPartialElevation(
    double[] coordinates,
    double begin,
    double end,
    double[] expectedCoordinates
  ) {
    var elevationProfile = new PackedCoordinateSequence.Double(coordinates, 2, 0);

    var partialElevationProfile = ElevationProfileSlicer.slice(elevationProfile, begin, end);

    if (expectedCoordinates == null) {
      assertNull(partialElevationProfile);
    } else {
      var expectedElevationProfile = new PackedCoordinateSequence.Double(
        expectedCoordinates,
        2,
        0
      ).toCoordinateArray();
      var actualElevationProfile = partialElevationProfile != null
        ? partialElevationProfile.toCoordinateArray()
        : null;
      assertArrayEquals(expectedElevationProfile, actualElevationProfile);
    }
  }
}
