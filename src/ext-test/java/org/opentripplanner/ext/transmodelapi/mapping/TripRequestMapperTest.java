package org.opentripplanner.ext.transmodelapi.mapping;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.transmodelapi.TransmodelRequestContext;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

public class TripRequestMapperTest implements PlanTestConstants {

  static final TransmodelRequestContext context;
  private static final Duration MAX_FLEXIBLE = Duration.ofMinutes(20);

  static {
    var graph = new Graph();
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.STOCKHOLM);
    final var transitService = new DefaultTransitService(transitModel);

    var defaultRequest = new RouteRequest();

    // Change defaults for FLEXIBLE to a lower value than the default 45m. This should restrict the
    // input to be less than 20m, not 45m.
    defaultRequest.withPreferences(pb ->
      pb.withStreet(sp ->
        sp
          .withMaxAccessEgressDuration(b -> b.with(StreetMode.FLEXIBLE, MAX_FLEXIBLE))
          .withMaxDirectDuration(b -> b.with(StreetMode.FLEXIBLE, MAX_FLEXIBLE))
      )
    );

    context =
      new TransmodelRequestContext(
        DefaultServerRequestContext.create(
          RouterConfig.DEFAULT.transitTuningConfig(),
          defaultRequest,
          RaptorConfig.defaultConfigForTest(),
          graph,
          transitService,
          Metrics.globalRegistry,
          RouterConfig.DEFAULT.vectorTileLayers(),
          new DefaultWorldEnvelopeService(new DefaultWorldEnvelopeRepository()),
          new DefaultVehiclePositionService(),
          new DefaultVehicleRentalService(),
          RouterConfig.DEFAULT.flexConfig(),
          List.of(),
          null
        ),
        null,
        transitService
      );
  }

  private static final List<Map<String, Object>> DURATIONS = List.of(
    Map.of("streetMode", StreetMode.WALK, "duration", Duration.ofMinutes(30)),
    Map.of("streetMode", StreetMode.BIKE, "duration", Duration.ofMinutes(31)),
    Map.of("streetMode", StreetMode.BIKE_TO_PARK, "duration", Duration.ofMinutes(32)),
    Map.of("streetMode", StreetMode.BIKE_RENTAL, "duration", Duration.ofMinutes(33)),
    Map.of("streetMode", StreetMode.SCOOTER_RENTAL, "duration", Duration.ofMinutes(34)),
    Map.of("streetMode", StreetMode.CAR, "duration", Duration.ofMinutes(35)),
    Map.of("streetMode", StreetMode.CAR_TO_PARK, "duration", Duration.ofMinutes(36)),
    Map.of("streetMode", StreetMode.CAR_PICKUP, "duration", Duration.ofMinutes(30)),
    Map.of("streetMode", StreetMode.CAR_RENTAL, "duration", Duration.ofMinutes(38)),
    // Same as max value for FLEXIBLE
    Map.of("streetMode", StreetMode.FLEXIBLE, "duration", MAX_FLEXIBLE)
  );

  @Test
  public void testMaxAccessEgressDurationForMode() {
    Map<String, Object> arguments = Map.of("maxAccessEgressDurationForMode", DURATIONS);

    var routeRequest = TripRequestMapper.createRequest(executionContext(arguments));
    assertNotNull(routeRequest);
    var preferences = routeRequest.preferences();
    assertNotNull(preferences);
    var streetPreferences = preferences.street();
    assertNotNull(streetPreferences);
    var maxAccessEgressDuration = streetPreferences.maxAccessEgressDuration();
    assertNotNull(maxAccessEgressDuration);

    for (var entry : DURATIONS) {
      var mode = (StreetMode) entry.get("streetMode");
      var expected = (Duration) entry.get("duration");
      assertEquals(expected, maxAccessEgressDuration.valueOf(mode), mode.name());
    }
  }

  @Test
  public void testMaxDirectDurationForMode() {
    Map<String, Object> arguments = Map.of("maxDirectDurationForMode", DURATIONS);

    var routeRequest = TripRequestMapper.createRequest(executionContext(arguments));
    assertNotNull(routeRequest);
    var preferences = routeRequest.preferences();
    assertNotNull(preferences);
    var streetPreferences = preferences.street();
    assertNotNull(streetPreferences);
    var maxDirectDuration = streetPreferences.maxDirectDuration();
    assertNotNull(maxDirectDuration);

    for (var entry : DURATIONS) {
      var streetMode = (StreetMode) entry.get("streetMode");
      var duration = (Duration) entry.get("duration");

      assertEquals(maxDirectDuration.valueOf(streetMode), duration);
    }
  }

  @Test
  public void testMaxAccessEgressDurationValidation() {
    var defaultValue = StreetPreferences.DEFAULT.maxAccessEgressDuration().valueOf(StreetMode.WALK);
    var duration = List.of(
      Map.of("streetMode", StreetMode.WALK, "duration", defaultValue.plusSeconds(1))
    );

    Map<String, Object> arguments = Map.of("maxAccessEgressDurationForMode", duration);

    assertThrows(
      IllegalArgumentException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testMaxAccessEgressDurationForFlexWithTooLongDuration() {
    Map<String, Object> arguments = Map.of(
      "maxAccessEgressDurationForMode",
      List.of(Map.of("streetMode", StreetMode.FLEXIBLE, "duration", MAX_FLEXIBLE.plusSeconds(1)))
    );
    assertThrows(
      IllegalArgumentException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testMaxDirectDurationValidation() {
    var defaultValue = StreetPreferences.DEFAULT.maxDirectDuration().valueOf(StreetMode.WALK);
    var duration = List.of(
      Map.of("streetMode", StreetMode.WALK, "duration", defaultValue.plusSeconds(1))
    );

    Map<String, Object> arguments = Map.of("maxDirectDurationForMode", duration);

    assertThrows(
      IllegalArgumentException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testMaxDirectDurationForFlexWithTooLongDuration() {
    Map<String, Object> arguments = Map.of(
      "maxDirectDurationForMode",
      List.of(Map.of("streetMode", StreetMode.FLEXIBLE, "duration", MAX_FLEXIBLE.plusSeconds(1)))
    );
    assertThrows(
      IllegalArgumentException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testBikeTriangleFactors() {
    Map<String, Object> arguments = Map.of(
      "bicycleOptimisationMethod",
      BicycleOptimizeType.TRIANGLE,
      "triangleFactors",
      Map.of("safety", 0.1, "slope", 0.1, "time", 0.8)
    );

    var req1 = TripRequestMapper.createRequest(executionContext(arguments));

    assertEquals(BicycleOptimizeType.TRIANGLE, req1.preferences().bike().optimizeType());
    assertEquals(
      new TimeSlopeSafetyTriangle(0.8, 0.1, 0.1),
      req1.preferences().bike().optimizeTriangle()
    );
  }

  @Test
  void testDefaultTriangleFactors() {
    var req2 = TripRequestMapper.createRequest(executionContext(Map.of()));
    assertEquals(BicycleOptimizeType.SAFE, req2.preferences().bike().optimizeType());
    assertEquals(TimeSlopeSafetyTriangle.DEFAULT, req2.preferences().bike().optimizeTriangle());
  }

  static Stream<Arguments> noTriangleCases = BicycleOptimizeType
    .nonTriangleValues()
    .stream()
    .map(Arguments::of);

  @ParameterizedTest
  @VariableSource("noTriangleCases")
  public void testBikeTriangleFactorsHasNoEffect(BicycleOptimizeType bot) {
    Map<String, Object> arguments = Map.of(
      "bicycleOptimisationMethod",
      bot,
      "triangleFactors",
      Map.of("safety", 0.1, "slope", 0.1, "time", 0.8)
    );

    var req1 = TripRequestMapper.createRequest(executionContext(arguments));

    assertEquals(bot, req1.preferences().bike().optimizeType());
    assertEquals(TimeSlopeSafetyTriangle.DEFAULT, req1.preferences().bike().optimizeTriangle());
  }

  private DataFetchingEnvironment executionContext(Map<String, Object> arguments) {
    ExecutionInput executionInput = ExecutionInput
      .newExecutionInput()
      .query("")
      .operationName("trip")
      .context(context)
      .locale(Locale.ENGLISH)
      .build();

    var executionContext = newExecutionContextBuilder()
      .executionInput(executionInput)
      .executionId(ExecutionId.from(this.getClass().getName()))
      .build();

    var env = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .context(context)
      .arguments(arguments)
      .build();

    return env;
  }
}
