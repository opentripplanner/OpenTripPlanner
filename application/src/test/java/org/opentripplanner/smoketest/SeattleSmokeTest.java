package org.opentripplanner.smoketest;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.client.model.RequestMode.BICYCLE;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.api.types.Route;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.LegMode;
import org.opentripplanner.client.model.TripPlan;
import org.opentripplanner.client.parameters.TripPlanParameters;
import org.opentripplanner.client.parameters.TripPlanParametersBuilder;
import org.opentripplanner.smoketest.util.RequestCombinationsBuilder;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

@Tag("smoke-test")
@Tag("seattle")
public class SeattleSmokeTest {

  private static final String CCSWW_ROUTE = "Volunteer Services: Northwest";
  static final Coordinate SODO = new Coordinate(47.5811, -122.3290);
  static final Coordinate CLYDE_HILL = new Coordinate(47.6316, -122.2173);

  static final Coordinate RONALD_BOG_PARK = new Coordinate(47.75601664, -122.33141);

  static final Coordinate ESPERANCE = new Coordinate(47.797330, -122.351560592);
  static final Coordinate SHORELINE = new Coordinate(47.7568, -122.3483);
  static final Coordinate MOUNTAINLAKE_TERRACE = new Coordinate(47.7900, -122.30379581);
  static final Coordinate OLIVE_WAY = new Coordinate(47.61309420, -122.336314916);

  @Test
  public void busWithFares() {
    var modes = Set.of(TRANSIT, WALK);
    var plan = SmokeTest.basicRouteTest(
      new SmokeTestRequest(OLIVE_WAY, SHORELINE, modes),
      List.of("WALK", "BUS", "WALK")
    );

    SmokeTest.assertThatAllTransitLegsHaveFareProductsHigherThanZero(plan);
  }

  @Nested
  class AccessibleRouting {

    @Test
    public void accessibleRouting() throws IOException {
      var tripPlan = testAccessibleRouting(1.6f);
      assertFalse(tripPlan.transitItineraries().isEmpty());
    }

    @Test
    public void accessibleRoutingWithVeryHighWalkReluctance() throws IOException {
      testAccessibleRouting(50);
    }

    private TripPlan testAccessibleRouting(double walkReluctance) throws IOException {
      var req = new TripPlanParametersBuilder()
        .withFrom(SODO)
        .withTo(CLYDE_HILL)
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
  }

  @Test
  public void flexAndTransit() {
    var modes = Set.of(WALK, BUS, FLEX_DIRECT, FLEX_EGRESS, FLEX_ACCESS);
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(SHORELINE, RONALD_BOG_PARK, modes),
      List.of("BUS")
    );
  }

  @Test
  public void ccswwIntoKingCounty() {
    var modes = Set.of(WALK, FLEX_DIRECT);
    var plan = SmokeTest.basicRouteTest(
      new SmokeTestRequest(ESPERANCE, SHORELINE, modes),
      List.of("BUS")
    );
    var itin = plan.itineraries().getFirst();
    var flexLeg = itin.transitLegs().getFirst();
    assertEquals(CCSWW_ROUTE, flexLeg.route().getLongName());
    assertEquals(CCSWW_ROUTE, flexLeg.route().getAgency().getName());
  }

  @Test
  public void ccswwIntoSnohomishCounty() {
    var modes = Set.of(WALK, FLEX_DIRECT);
    var plan = SmokeTest.basicRouteTest(
      new SmokeTestRequest(SHORELINE, ESPERANCE, modes),
      List.of("BUS", "WALK")
    );
    var walkAndFlex = plan
      .transitItineraries()
      .stream()
      .filter(i ->
        i.transitLegs().stream().anyMatch(l -> l.route().getLongName().equals(CCSWW_ROUTE))
      )
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
    var modes = SmokeTest.API_CLIENT.routes()
      .stream()
      .map(Route::getMode)
      .map(Objects::toString)
      .collect(Collectors.toSet());
    // for some reason the monorail feed says its route is of type rail
    assertEquals(Set.of("TRAM", "FERRY", "BUS", "RAIL"), modes);
  }

  @Test
  public void sharedStop() throws IOException {
    var tpr = TripPlanParameters.builder()
      .withFrom(OLIVE_WAY)
      .withTo(MOUNTAINLAKE_TERRACE)
      .withModes(BUS, WALK)
      .withTime(SmokeTest.weekdayAtNoon().withHour(14).withMinute(30))
      .build();
    var plan = SmokeTest.API_CLIENT.plan(tpr);
    var itineraries = plan.itineraries();

    var first = itineraries.getFirst();
    var leg = first.transitLegs().getFirst();
    assertThat(Set.of("510", "415")).contains(leg.route().getShortName());
    assertThat(Set.of("Sound Transit", "Community Transit")).contains(
      leg.route().getAgency().getName()
    );

    var stop = leg.from().stop().get();
    assertEquals("Olive Way & 6th Ave", stop.name());
    assertEquals("kcm:1040", stop.id());
    assertEquals("1040", stop.code().get());

    SmokeTest.assertThatAllTransitLegsHaveFareProducts(plan);
  }

  static List<TripPlanParameters> buildCombinations() {
    return new RequestCombinationsBuilder()
      .withLocations(SODO, ESPERANCE, CLYDE_HILL, RONALD_BOG_PARK, OLIVE_WAY, MOUNTAINLAKE_TERRACE)
      .withModes(TRANSIT, WALK)
      .withTime(SmokeTest.weekdayAtNoon())
      .includeWheelchair()
      .includeArriveBy()
      .build();
  }

  @ParameterizedTest
  @MethodSource("buildCombinations")
  public void accessibleRouting(TripPlanParameters params) throws IOException {
    var tripPlan = SmokeTest.API_CLIENT.plan(params);
    assertFalse(tripPlan.transitItineraries().isEmpty());
  }

  static List<TripPlanParameters> bikeCombinations() {
    return new RequestCombinationsBuilder()
      .withLocations(SODO, ESPERANCE, OLIVE_WAY, MOUNTAINLAKE_TERRACE)
      .withModes(TRANSIT, BICYCLE)
      .withTime(SmokeTest.weekdayAtNoon())
      .includeArriveBy()
      .build();
  }

  @ParameterizedTest
  @MethodSource("bikeCombinations")
  public void bikeAndTransit(TripPlanParameters params) throws IOException {
    var tripPlan = SmokeTest.API_CLIENT.plan(params);
    assertFalse(tripPlan.transitItineraries().isEmpty());
  }

  @Test
  public void vehiclePositions() {
    SmokeTest.assertThereArePatternsWithVehiclePositions();
  }
}
