package org.opentripplanner.smoketest;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.WgsCoordinate;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 */
@Tag("smoke-test")
@Tag("septa")
public class SeptaSmokeTest {

  WgsCoordinate airport = new WgsCoordinate(39.876151, -75.245189);
  WgsCoordinate stPetersCemetary = new WgsCoordinate(39.98974, -75.09515);

  WgsCoordinate pierceStreet = new WgsCoordinate(39.93014, -75.18047);
  WgsCoordinate templeUniversity = new WgsCoordinate(39.98069, -75.14886);

  @Test
  public void routeFromAirportToNorthPhiladelphia() {
    SmokeTest.basicRouteTest(
      airport,
      stPetersCemetary,
      Set.of("TRANSIT", "WALK"),
      List.of("WALK", "RAIL", "RAIL", "WALK", "SUBWAY", "WALK")
    );
  }

  @Test
  public void vehiclePositions() {
    SmokeTest.assertThereArePatternsWithVehiclePositions();
  }

  @Test
  public void bikeRentalStations() {
    SmokeTest.assertThatThereAreVehicleRentalStations();
  }

  @Test
  public void routeWithBikeRental() {
    SmokeTest.basicRouteTest(
      pierceStreet,
      templeUniversity,
      Set.of("BICYCLE_RENT"),
      List.of("WALK", "BICYCLE", "WALK")
    );
  }
}
