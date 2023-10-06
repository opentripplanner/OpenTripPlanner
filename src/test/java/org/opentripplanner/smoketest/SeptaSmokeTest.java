package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.client.model.RequestMode.BICYCLE_RENT;
import static org.opentripplanner.client.model.RequestMode.TRANSIT;
import static org.opentripplanner.client.model.RequestMode.WALK;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.TripPlan.FareProductUse;
import org.opentripplanner.smoketest.util.SmokeTestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 */
@Tag("smoke-test")
@Tag("septa")
public class SeptaSmokeTest {

  private static final Logger LOG = LoggerFactory.getLogger(SeptaSmokeTest.class);

  Coordinate airport = new Coordinate(39.876151, -75.245189);
  Coordinate stPetersCemetary = new Coordinate(39.98974, -75.09515);

  Coordinate pierceStreet = new Coordinate(39.93014, -75.18047);
  Coordinate templeUniversity = new Coordinate(39.98069, -75.14886);

  @Test
  public void routeFromAirportToNorthPhiladelphia() {
    var modes = Set.of(TRANSIT, WALK);
    var plan = SmokeTest.basicRouteTest(
      new SmokeTestRequest(airport, stPetersCemetary, modes),
      List.of("WALK", "RAIL", "RAIL", "WALK", "SUBWAY", "WALK")
    );
    var products = plan
      .itineraries()
      .stream()
      .flatMap(i -> i.legs().stream())
      .flatMap(leg -> leg.fareProducts().stream())
      .map(FareProductUse::product)
      .toList();

    assertFalse(products.isEmpty());

    var prices = products
      .stream()
      .map(p -> p.price().amount().doubleValue())
      .collect(Collectors.toSet());

    LOG.info("Received fare products {}", products);

    assertTrue(prices.contains(2.5d));
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
      new SmokeTestRequest(pierceStreet, templeUniversity, Set.of(BICYCLE_RENT)),
      List.of("WALK", "BICYCLE", "WALK")
    );
  }
}
