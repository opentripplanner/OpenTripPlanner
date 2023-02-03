package org.opentripplanner.smoketest;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;

@Tag("smoke-test")
@Tag("portland")
public class PortlandSmokeTest {

  WgsCoordinate cennentenial = new WgsCoordinate(45.504602, -122.4968719);
  WgsCoordinate hillside = new WgsCoordinate(45.5275, -122.7110);

  @Test
  public void railRouteAcrossTheCity() {
    SmokeTest.basicRouteTest(
      cennentenial,
      hillside,
      Set.of("TRAM", "WALK"),
      List.of("WALK", "TRAM", "WALK")
    );
  }

  @Test
  public void geofencingZone() {
    SmokeTest.basicRouteTest(
      cennentenial,
      hillside,
      Set.of("SCOOTER_RENT", "WALK"),
      List.of("WALK", "SCOOTER", "WALK")
    );
  }
}
