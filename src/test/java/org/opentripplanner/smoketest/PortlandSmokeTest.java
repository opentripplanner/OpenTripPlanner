package org.opentripplanner.smoketest;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

@Tag("smoke-test")
@Tag("portland")
public class PortlandSmokeTest {

  WgsCoordinate cennentenial = new WgsCoordinate(45.504602, -122.4968719);
  WgsCoordinate hillside = new WgsCoordinate(45.5275, -122.7110);

  @Test
  public void railRouteAcrossTheCity() {
    Set<String> modes = Set.of("TRAM", "WALK");
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(cennentenial, hillside, modes),
      List.of("WALK", "TRAM", "WALK")
    );
  }

  /**
   * Checks that a scooter rental finishes at the edge of the business area and is continued on
   * foot rather than scootering all the way to the destination.
   */
  @Test
  public void geofencingZone() {
    Set<String> modes = Set.of("SCOOTER_RENT", "WALK");
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(cennentenial, hillside, modes),
      List.of("WALK", "SCOOTER", "WALK")
    );
  }
}
