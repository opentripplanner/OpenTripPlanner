package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.smoketest.SmokeTest.assertThatItineraryHasModes;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.WgsCoordinate;

@Tag("smoke-test")
@Tag("houston")
public class HoustonSmokeTest {

  WgsCoordinate galvestonRoad = new WgsCoordinate(29.6598, -95.2342);
  WgsCoordinate northLindale = new WgsCoordinate(29.8158, -95.3697);

  @Test
  public void routeFromSouthToNorth() {
    var params = new SmokeTestRequest(galvestonRoad, northLindale, Set.of("TRANSIT", "WALK"));
    var otpResponse = SmokeTest.sendPlanRequest(params);
    var itineraries = otpResponse.getPlan().itineraries;

    assertTrue(itineraries.size() > 1);

    var expectedModes = List.of("WALK", "SUBWAY", "WALK", "BUS", "WALK", "BUS", "WALK");
    assertThatItineraryHasModes(itineraries, expectedModes);
  }
}
