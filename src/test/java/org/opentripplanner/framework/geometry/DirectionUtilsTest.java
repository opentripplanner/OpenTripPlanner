package org.opentripplanner.framework.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.geotools.referencing.GeodeticCalculator;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

public class DirectionUtilsTest {

  @Test
  public final void testAzimuth() {
    final int N_RUN = 1000000;

    GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

    Random rand = new Random(42);
    List<Coordinate> from = new ArrayList<>(N_RUN);
    List<Coordinate> to = new ArrayList<>(N_RUN);

    // Add fixed points
    from.add(new Coordinate(0, 45));
    to.add(new Coordinate(0, 45)); // Undefined: 180 deg
    assertEquals(180, DirectionUtils.getAzimuth(from.get(0), to.get(0)));

    from.add(new Coordinate(0, 45));
    to.add(new Coordinate(0.1, 45)); // East: 90 deg
    assertEquals(90, DirectionUtils.getAzimuth(from.get(1), to.get(1)));

    from.add(new Coordinate(0, 45));
    to.add(new Coordinate(0, 45.1)); // North: 0 deg
    assertEquals(0, DirectionUtils.getAzimuth(from.get(2), to.get(2)));

    from.add(new Coordinate(0, 45));
    to.add(new Coordinate(-0.1, 45)); // West: -90 deg
    assertEquals(-90, DirectionUtils.getAzimuth(from.get(3), to.get(3)));

    from.add(new Coordinate(0, 45));
    to.add(new Coordinate(0, 44.9)); // South: 180 deg
    assertEquals(180, DirectionUtils.getAzimuth(from.get(4), to.get(4)));

    for (int i = 0; i < N_RUN; i++) {
      Coordinate a = new Coordinate(rand.nextDouble() * 0.1, 45 + rand.nextDouble() * 0.1);
      Coordinate b = new Coordinate(rand.nextDouble() * 0.1, 45 + rand.nextDouble() * 0.1);
      from.add(a);
      to.add(b);
    }

    double[] exactAzimuths = new double[from.size()];
    double[] approxAzimuths = new double[to.size()];

    long start = System.currentTimeMillis();
    for (int i = 0; i < from.size(); i++) {
      geodeticCalculator.setStartingGeographicPoint(from.get(i).x, from.get(i).y);
      geodeticCalculator.setDestinationGeographicPoint(to.get(i).x, to.get(i).y);
      exactAzimuths[i] = geodeticCalculator.getAzimuth();
    }
    long exactTimeMs = System.currentTimeMillis() - start;
    System.out.println(
      "GeodeticCalculator exact azimuth: " + exactTimeMs + "ms for " + N_RUN + " computations."
    );

    start = System.currentTimeMillis();
    for (int i = 0; i < from.size(); i++) {
      approxAzimuths[i] = DirectionUtils.getAzimuth(from.get(i), to.get(i));
    }
    long approxTimeMs = System.currentTimeMillis() - start;
    System.out.println(
      "UtilsDistance approx azimuth: " + approxTimeMs + "ms for " + N_RUN + " computations."
    );

    double maxError = 0.0;
    for (int i = 0; i < exactAzimuths.length; i++) {
      double error = (exactAzimuths[i] - approxAzimuths[i]); // Degrees
      if (error > 360) error -= 360;
      if (error < -360) error += 360;
      if (error > maxError) maxError = error;
    }
    System.out.println("Max error in azimuth: " + maxError + " degrees.");
    assertTrue(maxError < 0.15);
  }
}
