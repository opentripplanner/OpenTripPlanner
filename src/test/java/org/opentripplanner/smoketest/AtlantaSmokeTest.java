package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.client.model.RequestMode.FLEX_EGRESS;
import static org.opentripplanner.client.model.RequestMode.TRANSIT;
import static org.opentripplanner.client.model.RequestMode.WALK;
import static org.opentripplanner.smoketest.SmokeTest.assertThatItineraryHasModes;
import static org.opentripplanner.smoketest.SmokeTest.basicRouteTest;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

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

  Coordinate nearGeorgiaStateStation = new Coordinate(33.74139944890028, -84.38607215881348);
  Coordinate powderSpringsInsideFlexZone1 = new Coordinate(33.86916840022388, -84.66315507888794);

  @Test
  public void regularRouteFromCentralAtlantaToPowderSprings() {
    var modes = Set.of(TRANSIT, WALK);
    SmokeTest.basicRouteTest(
      new SmokeTestRequest(nearGeorgiaStateStation, powderSpringsInsideFlexZone1, modes),
      List.of("WALK", "SUBWAY", "WALK", "BUS", "WALK", "BUS", "WALK")
    );
  }

  @Test
  public void flexRouteFromCentralAtlantaToPowderSprings() {
    var req = new SmokeTestRequest(
      nearGeorgiaStateStation,
      powderSpringsInsideFlexZone1,
      Set.of(FLEX_EGRESS, WALK, TRANSIT)
    );

    var expectedModes = List.of("WALK", "SUBWAY", "WALK", "BUS", "WALK", "BUS");
    var plan = basicRouteTest(req, expectedModes);
    var itineraries = plan.itineraries();

    assertThatItineraryHasModes(itineraries, expectedModes);

    var transitLegs = itineraries
      .stream()
      .flatMap(i -> i.legs().stream().filter(l -> l.route() != null))
      .toList();

    var usesZone1Route = transitLegs
      .stream()
      .map(l -> l.route().name())
      .anyMatch(name -> name.equals("Zone 1"));

    assertTrue(usesZone1Route);
  }
}
