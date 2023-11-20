package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.client.model.RequestMode.BUS;
import static org.opentripplanner.client.model.RequestMode.FLEX_ACCESS;
import static org.opentripplanner.client.model.RequestMode.FLEX_DIRECT;
import static org.opentripplanner.client.model.RequestMode.FLEX_EGRESS;
import static org.opentripplanner.client.model.RequestMode.TRANSIT;
import static org.opentripplanner.client.model.RequestMode.WALK;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.LegMode;
import org.opentripplanner.client.model.Route;
import org.opentripplanner.client.model.TripPlan;
import org.opentripplanner.client.parameters.TripPlanParametersBuilder;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

@Tag("smoke-test")
@Tag("seattle")
public class SeattleSmokeTest {

  private static final String CCSWW_ROUTE = "Volunteer Services: Northwest";
  Coordinate sodo = new Coordinate(47.5811, -122.3290);
  Coordinate clydeHill = new Coordinate(47.6316, -122.2173);

  Coordinate ronaldBogPark = new Coordinate(47.75601664, -122.33141);

  Coordinate esperance = new Coordinate(47.7957, -122.3470);
  Coordinate shoreline = new Coordinate(47.7568, -122.3483);

  @Test
  public void acrossTheCity() {
    var modes = Set.of(TRANSIT, WALK);
    var plan = SmokeTest.basicRouteTest(
      new SmokeTestRequest(sodo, clydeHill, modes),
      List.of("WALK", "BUS", "WALK", "BUS", "WALK")
    );

    SmokeTest.assertThatAllTransitLegsHaveFareProducts(plan);
  }

  @Test
  public void accessibleRouting() throws IOException {
    var tripPlan = testAccessibleRouting(1.6f);
    assertFalse(tripPlan.transitItineraries().isEmpty());
  }

  @Test
  @Disabled
  public void accessibleRoutingWithVeryHighWalkReluctance() throws IOException {
    testAccessibleRouting(50);
  }

  private TripPlan testAccessibleRouting(float walkReluctance) throws IOException {
    var req = new TripPlanParametersBuilder()
      .withFrom(sodo)
      .withTo(clydeHill)
      .withTime(SmokeTest.weekdayAtNoon())
      .withWheelchair(true)
      .withModes(TRANSIT)
      .withWalkReluctance(walkReluctance)
      .build();

    var tripPlan = SmokeTest.API_CLIENT.plan(req);

    // assert that accessibility score is there
    tripPlan
      .itineraries()
      .forEach(i -> {
        assertTrue(i.accessibilityScore().isPresent());
        i.legs().forEach(l -> assertTrue(l.accessibilityScore().isPresent()));
      });
    return tripPlan;
  }

  @Test
  public void flexAndTransit() {
    var modes = Set.of(WALK, BUS, FLEX_DIRECT, FLEX_EGRESS, FLEX_ACCESS);
    SmokeTest.basicRouteTest(new SmokeTestRequest(shoreline, ronaldBogPark, modes), List.of("BUS"));
  }

  @Test
  public void ccswwIntoKingCounty() {
    var modes = Set.of(WALK, FLEX_DIRECT);
    var plan = SmokeTest.basicRouteTest(
      new SmokeTestRequest(esperance, shoreline, modes),
      List.of("BUS")
    );
    var itin = plan.itineraries().get(0);
    var flexLeg = itin.transitLegs().get(0);
    assertEquals(CCSWW_ROUTE, flexLeg.route().name());
    assertEquals(CCSWW_ROUTE, flexLeg.route().agency().name());
  }

  @Test
  public void ccswwIntoSnohomishCounty() {
    var modes = Set.of(WALK, FLEX_DIRECT);
    var plan = SmokeTest.basicRouteTest(
      new SmokeTestRequest(shoreline, esperance, modes),
      List.of("BUS", "WALK")
    );
    var walkAndFlex = plan
      .transitItineraries()
      .stream()
      .filter(i -> i.transitLegs().stream().anyMatch(l -> l.route().name().equals(CCSWW_ROUTE)))
      .findFirst()
      .get();
    assertEquals(2, walkAndFlex.legs().size());
    // walk to the border of King County
    assertEquals(LegMode.WALK, walkAndFlex.legs().get(0).mode());
    // and take flex inside Snohomish County to the destination
    assertEquals(LegMode.BUS, walkAndFlex.legs().get(1).mode());
  }

  @Test
  public void monorailRoute() throws IOException {
    var modes = SmokeTest.API_CLIENT
      .routes()
      .stream()
      .map(Route::mode)
      .map(Objects::toString)
      .collect(Collectors.toSet());
    assertEquals(Set.of("MONORAIL", "TRAM", "FERRY", "BUS"), modes);
  }

  @Test
  public void vehiclePositions() {
    SmokeTest.assertThereArePatternsWithVehiclePositions();
  }
}
