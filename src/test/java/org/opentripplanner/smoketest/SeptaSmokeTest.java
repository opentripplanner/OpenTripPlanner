package org.opentripplanner.smoketest;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.WgsCoordinate;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 * <p>
 * It uses the REST API to check that a route from South to North Houston can be found.
 */
@Tag("septa")
@Tag("houston")
public class SeptaSmokeTest {

  WgsCoordinate woodbury = new WgsCoordinate(39.8974, -75.1417);
  WgsCoordinate logan = new WgsCoordinate(40.0294, -75.1449);

  @Test
  public void routeFromSouthToNorth() {
    SmokeTest.basicTest(
      woodbury,
      logan,
      Set.of("TRANSIT", "WALK"),
      List.of("WALK", "BUS", "BUS", "WALK", "BUS", "WALK")
    );
  }
}
