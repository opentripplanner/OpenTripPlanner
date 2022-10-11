package org.opentripplanner.smoketest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import org.opentripplanner.api.json.JSONObjectMapperProvider;
import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.smoketest.util.GraphQLClient;
import org.opentripplanner.smoketest.util.RestClient;
import org.opentripplanner.smoketest.util.SmokeTestRequest;
import org.opentripplanner.transit.model.basic.WgsCoordinate;

/**
 * This is both a utility class and a category to select or deselect smoke tests during test
 * execution.
 * <p>
 * By default, the smoke tests are not run when you execute `mvn test`.
 * <p>
 * If you want run them, use `mvn test -Djunit.tags.included="atlanta" -Djunit.tags.excluded=""`.
 */
public class SmokeTest {

  public static final ObjectMapper mapper;

  static {
    var provider = new JSONObjectMapperProvider();

    SimpleModule module = new SimpleModule("SmokeTests");
    module.addDeserializer(ItineraryFares.class, new FareDeserializer());

    mapper = provider.getContext(null);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
    mapper.registerModule(module);
  }

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
    return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
  }

  public static void assertThatThereAreVehicleRentalStations() {
    var stations = GraphQLClient.vehicleRentalStations();
    assertFalse(stations.isEmpty(), "Found no vehicle rental stations.");
  }

  /**
   * Given a list of itineraries assert that at least one of them has legs that have the expected
   * modes.
   */
  static void assertThatItineraryHasModes(
    List<ApiItinerary> itineraries,
    List<String> expectedModes
  ) {
    var itineraryModes = itineraries
      .stream()
      .map(i -> i.legs.stream().map(l -> l.mode).toList())
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

  static void basicRouteTest(
    WgsCoordinate start,
    WgsCoordinate end,
    Set<String> modes,
    List<String> expectedModes
  ) {
    var request = new SmokeTestRequest(start, end, modes);
    var otpResponse = RestClient.sendPlanRequest(request);
    var itineraries = otpResponse.getPlan().itineraries;

    assertTrue(itineraries.size() >= 1);

    assertThatItineraryHasModes(itineraries, expectedModes);
  }

  static void assertThereArePatternsWithVehiclePositions() {
    GraphQLClient.VehiclePositionResponse positions = GraphQLClient.patternWithVehiclePositionsQuery();

    var vehiclePositions = positions
      .patterns()
      .stream()
      .flatMap(p -> p.vehiclePositions().stream())
      .toList();

    assertFalse(
      vehiclePositions.isEmpty(),
      "Found no patterns that have realtime vehicle positions."
    );
  }

  /**
   * The Fare class is a little hard to deserialize, so we have a custom deserializer as we don't
   * run any assertions against the fares. (That is done during unit tests.)
   */
  static class FareDeserializer extends JsonDeserializer<ItineraryFares> {

    @Override
    public ItineraryFares deserialize(
      JsonParser jsonParser,
      DeserializationContext deserializationContext
    ) {
      return null;
    }
  }
}
