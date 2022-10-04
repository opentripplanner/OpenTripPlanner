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
@Tag("smoke-test")
@Tag("septa")
public class SeptaSmokeTest {

  WgsCoordinate airport = new WgsCoordinate(39.876151, -75.245189);
  WgsCoordinate stPetersCemetary = new WgsCoordinate(39.98974, -75.09515);

  @Test
  public void routeFromAirportToNorthPhiladelphia() {
    SmokeTest.basicTest(
      airport,
      stPetersCemetary,
      Set.of("TRANSIT", "WALK"),
      List.of("WALK", "RAIL", "RAIL", "WALK", "SUBWAY", "WALK", "BUS", "WALK")
    );
  }
}
