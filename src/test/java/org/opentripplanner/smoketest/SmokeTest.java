package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.opentripplanner.client.OtpApiClient;
import org.opentripplanner.client.model.Itinerary;
import org.opentripplanner.client.model.TripPlan;
import org.opentripplanner.client.model.VehicleRentalStation;
import org.opentripplanner.client.parameters.TripPlanParameters;
import org.opentripplanner.smoketest.util.SmokeTestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is both a utility class and a category to select or deselect smoke tests during test
 * execution.
 * <p>
 * By default, the smoke tests are not run when you execute `mvn test`.
 * <p>
 * If you want run them, use `mvn test -Djunit.tags.included="atlanta" -Djunit.tags.excluded=""`.
 */
public class SmokeTest {

  private static final Logger LOG = LoggerFactory.getLogger(SmokeTest.class);

  public static final OtpApiClient API_CLIENT = new OtpApiClient(
    ZoneId.of("America/New_York"),
    "http://localhost:8080"
  );

  /**
   * In order to have somewhat predictable results we get the route for the next Monday.
   * <p>
   * When we approach the end of the validity of the GTFS feed there might be days when this logic
   * results in failures as the next Monday is after the end of the service period.
   * <p>
   * This is a problem in particular in the case of MARTA as they only publish new data about 2 days
   * before the expiration date of the old one.
   */
  public static LocalDate nextMonday() {
    var today = LocalDate.now();
    return today.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
  }

  public static void assertThatThereAreVehicleRentalStations() {
    try {
      List<VehicleRentalStation> stations = API_CLIENT.vehicleRentalStations();
      assertFalse(stations.isEmpty(), "Found no vehicle rental stations.");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Given a list of itineraries assert that at least one of them has legs that have the expected
   * modes.
   */
  static void assertThatItineraryHasModes(List<Itinerary> itineraries, List<String> expectedModes) {
    var itineraryModes = itineraries
      .stream()
      .map(i -> i.legs().stream().map(l -> l.mode().toString()).toList())
      .toList();
    assertTrue(
      itineraryModes.contains(expectedModes),
      String.format(
        "Could not find a mode combination '%s' in itineraries %s",
        expectedModes,
        itineraryModes
      )
    );
  }

  static TripPlan basicRouteTest(SmokeTestRequest req, List<String> expectedModes) {
    try {
      var tpr = TripPlanParameters
        .builder()
        .withFrom(req.from())
        .withTo(req.to())
        .withModes(req.modes())
        .withTime(SmokeTest.nextMonday().atTime(LocalTime.of(12, 0)))
        .withSearchDirection(req.searchDirection())
        .build();
      var plan = API_CLIENT.plan(tpr);
      var itineraries = plan.itineraries();

      assertFalse(itineraries.isEmpty(), "Expected to see some itineraries but got zero.");

      assertThatItineraryHasModes(itineraries, expectedModes);
      return plan;
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  static void assertThereArePatternsWithVehiclePositions() {
    try {
      var patterns = API_CLIENT.patterns();
      var vehiclePositions = patterns.stream().flatMap(p -> p.vehiclePositions().stream()).toList();

      assertFalse(
        vehiclePositions.isEmpty(),
        "Found no patterns that have realtime vehicle positions."
      );
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  static void assertThatAllTransitLegsHaveFareProducts(TripPlan plan) {
    var transitLegs = plan.transitItineraries().stream().flatMap(i -> i.transitLegs().stream());
    transitLegs.forEach(leg -> {
      assertFalse(leg.fareProducts().isEmpty(), "Leg %s should have fare products".formatted(leg));

      LOG.info(
        "Leg with mode {} from {} to {} has {} fare products.",
        leg.mode(),
        leg.from(),
        leg.to(),
        leg.fareProducts().size()
      );
    });
  }
}
