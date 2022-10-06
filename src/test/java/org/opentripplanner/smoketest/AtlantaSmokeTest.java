package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.smoketest.SmokeTest.assertThatItineraryHasModes;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.smoketest.util.RestClient;
import org.opentripplanner.smoketest.util.SmokeTestRequest;
import org.opentripplanner.transit.model.basic.WgsCoordinate;

/**
 * This smoke test expects an OTP installation running at localhost:8080
 * <p>
 * It uses the REST API to check that a route from central Atlanta to the flex zone in Powder
 * Springs can be planned. In order to guarantee somewhat predictable results over time it uses 1pm
 * on the next Monday relative to the day the test is run as the start time of the route.
 * <p>
 * The assertions are intentionally vague as more precise ones would probably cause false positives
 * when there are slight changes in the schedule.
 */
@Tag("smoke-test")
@Tag("atlanta")
public class AtlantaSmokeTest {

  WgsCoordinate nearGeorgiaStateStation = new WgsCoordinate(33.74139944890028, -84.38607215881348);
  WgsCoordinate powderSpringsInsideFlexZone1 = new WgsCoordinate(
    33.86916840022388,
    -84.66315507888794
  );

  @Test
  public void regularRouteFromCentralAtlantaToPowderSprings() {
    SmokeTest.basicRouteTest(
      nearGeorgiaStateStation,
      powderSpringsInsideFlexZone1,
      Set.of("TRANSIT", "WALK"),
      List.of("WALK", "SUBWAY", "WALK", "BUS", "WALK", "BUS", "WALK")
    );
  }

  @Test
  public void flexRouteFromCentralAtlantaToPowderSprings() {
    var params = new SmokeTestRequest(
      nearGeorgiaStateStation,
      powderSpringsInsideFlexZone1,
      Set.of("FLEX_EGRESS", "WALK", "TRANSIT")
    );
    var otpResponse = RestClient.sendPlanRequest(params);
    var itineraries = otpResponse.getPlan().itineraries;

    assertTrue(itineraries.size() > 0);

    var expectedModes = List.of("WALK", "SUBWAY", "WALK", "BUS", "WALK", "BUS");
    assertThatItineraryHasModes(itineraries, expectedModes);

    var transitLegs = itineraries
      .stream()
      .flatMap(i -> i.legs.stream().filter(l -> l.transitLeg))
      .toList();

    var usesZone1Route = transitLegs
      .stream()
      .map(l -> l.routeShortName)
      .anyMatch(name -> name.equals("Zone 1"));

    assertTrue(usesZone1Route);
  }
}
