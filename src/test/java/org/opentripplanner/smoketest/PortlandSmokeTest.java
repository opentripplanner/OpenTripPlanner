package org.opentripplanner.smoketest;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

@Tag("smoke-test")
@Tag("portland")
public class PortlandSmokeTest {

  WgsCoordinate cennentenial = new WgsCoordinate(45.504602, -122.4968719);
  WgsCoordinate hillside = new WgsCoordinate(45.5275, -122.7110);
  WgsCoordinate piedmont = new WgsCoordinate(45.5746, -122.6697);

  @Test
  public void railRouteAcrossTheCity() {
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(cennentenial, hillside, Set.of("TRAM", "WALK")),
      List.of("WALK", "TRAM", "WALK")
    );
  }

  /**
   * Checks that a scooter rental finishes at the edge of the business area and is continued on
   * foot rather than scootering all the way to the destination.
   */
  @ParameterizedTest(name = "scooter rental with arriveBy={0}")
  @ValueSource(booleans = { true, false })
  public void geofencingZone(boolean arriveBy) {
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(cennentenial, hillside, Set.of("SCOOTER_RENT", "WALK"), arriveBy),
      List.of("WALK", "SCOOTER", "WALK")
    );
  }

  @ParameterizedTest(name = "scooter rental with arriveBy={0}")
  @ValueSource(booleans = { true, false })
  void scooterRent(boolean arriveBy) {
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(cennentenial, piedmont, Set.of("SCOOTER_RENT", "WALK"), arriveBy),
      List.of("WALK", "SCOOTER")
    );
  }
}
