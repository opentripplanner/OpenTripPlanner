package org.opentripplanner.smoketest;

import static org.opentripplanner.client.model.RequestMode.BICYCLE;
import static org.opentripplanner.client.model.RequestMode.BUS;
import static org.opentripplanner.client.model.RequestMode.TRANSIT;
import static org.opentripplanner.client.model.RequestMode.WALK;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 * <p>
 * It uses the REST API to check that a route from South to North Houston can be found.
 */
@Tag("smoke-test")
@Tag("houston")
public class HoustonSmokeTest {

  Coordinate galvestonRoad = new Coordinate(29.6598, -95.2342);
  Coordinate northLindale = new Coordinate(29.8158, -95.3697);

  @Test
  public void routeFromSouthToNorth() {
    var modes = Set.of(TRANSIT, WALK);
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(galvestonRoad, northLindale, modes),
      List.of("WALK", "BUS", "BUS", "WALK", "TRAM", "WALK")
    );
  }

  @Test
  public void selectOnlyBusses() {
    var modes = Set.of(BUS, WALK);
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(galvestonRoad, northLindale, modes),
      List.of("WALK", "BUS", "BUS", "WALK", "BUS", "WALK")
    );
  }

  @Test
  public void bikeRoute() {
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(galvestonRoad, northLindale, Set.of(BICYCLE)),
      List.of("BICYCLE")
    );
  }
}
