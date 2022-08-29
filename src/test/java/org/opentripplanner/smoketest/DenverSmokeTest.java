package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.smoketest.SmokeTest.assertThatItineraryHasModes;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.WgsCoordinate;

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
    var request = new SmokeTestRequest(southBroadway, twinLakes, Set.of("TRANSIT", "WALK"));
    var otpResponse = SmokeTest.sendPlanRequest(request);
    var itineraries = otpResponse.getPlan().itineraries;

    assertTrue(itineraries.size() > 1);

    var expectedModes = List.of("WALK", "TRAM", "WALK", "BUS", "WALK");
    assertThatItineraryHasModes(itineraries, expectedModes);
  }
}
