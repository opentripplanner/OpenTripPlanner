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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.transmodelapi.TransmodelRequestContext;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.StopFinderTraverseVisitor;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

public class TripRequestMapperTest implements PlanTestConstants {

  static final TransmodelRequestContext context;

  static {
    var graph = new Graph();
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.STOCKHOLM);
    final var transitService = new DefaultTransitService(transitModel);

    context =
      new TransmodelRequestContext(
        DefaultServerRequestContext.create(
          RouterConfig.DEFAULT.transitTuningConfig(),
          new RouteRequest(),
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
    Map.of("streetMode", StreetMode.FLEXIBLE, "duration", Duration.ofMinutes(39))
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
      var streetMode = (StreetMode) entry.get("streetMode");
      var duration = (Duration) entry.get("duration");

      assertEquals(maxAccessEgressDuration.valueOf(streetMode), duration);
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
    var duration = List.of(
      Map.of("streetMode", StreetMode.WALK, "duration", Duration.ofDays(9999))
    );

    Map<String, Object> arguments = Map.of("maxAccessEgressDurationForMode", duration);

    assertThrows(
      IllegalArgumentException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testMaxDirectDurationValidation() {
    var duration = List.of(
      Map.of("streetMode", StreetMode.WALK, "duration", Duration.ofDays(9999))
    );

    Map<String, Object> arguments = Map.of("maxDirectDurationForMode", duration);

    assertThrows(
      IllegalArgumentException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
    );
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
