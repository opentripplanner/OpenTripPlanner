package org.opentripplanner.smoketest;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 * <p>
 * It uses the REST API to check that a route from South to North Denver can be found.
 */
@Tag("smoke-test")
@Tag("denver")
public class DenverSmokeTest {

  WgsCoordinate southBroadway = new WgsCoordinate(39.7020, -104.9866);
  WgsCoordinate twinLakes = new WgsCoordinate(39.8232, -105.0055);

  @Test
  public void routeFromSouthToNorth() {
    Set<String> modes = Set.of("TRANSIT", "WALK");
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(southBroadway, twinLakes, modes),
      List.of("WALK", "TRAM", "WALK", "BUS", "WALK")
    );
  }

  @Test
  public void vehiclePositions() {
    SmokeTest.assertThereArePatternsWithVehiclePositions();
  }
}
