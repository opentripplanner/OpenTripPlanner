package org.opentripplanner.ext.transmodelapi.mapping;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.transmodelapi.model.framework.StreetModeDurationInputType.FIELD_DURATION;
import static org.opentripplanner.ext.transmodelapi.model.framework.StreetModeDurationInputType.FIELD_STREET_MODE;
import static org.opentripplanner.ext.transmodelapi.model.plan.TripQuery.MAX_ACCESS_EGRESS_DURATION_FOR_MODE;
import static org.opentripplanner.ext.transmodelapi.model.plan.TripQuery.MAX_DIRECT_DURATION_FOR_MODE;
import static org.opentripplanner.ext.transmodelapi.model.plan.TripQuery.MIN_ACCESS_EGRESS_DURATION_FOR_MODE;
import static org.opentripplanner.routing.api.request.StreetMode.CAR;
import static org.opentripplanner.routing.api.request.StreetMode.FLEXIBLE;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;

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
import org.opentripplanner.routing.graph.Graph;
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
  private static final Duration MIN_DEFAULT = Duration.ofMinutes(1);
  private static final Duration MAX_DEFAULT = Duration.ofMinutes(45);
  private static final Duration MIN_FLEXIBLE = Duration.ofMinutes(5);
  private static final Duration MAX_FLEXIBLE = Duration.ofMinutes(20);
  private static final Duration D10m = Duration.ofMinutes(10);

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
          .withMinAccessEgressDuration(b -> b.withDefault(MIN_DEFAULT).with(FLEXIBLE, MIN_FLEXIBLE))
          .withMaxAccessEgressDuration(b -> b.withDefault(MAX_DEFAULT).with(FLEXIBLE, MAX_FLEXIBLE))
          .withMaxDirectDuration(b -> b.withDefault(MAX_DEFAULT).with(FLEXIBLE, MAX_FLEXIBLE))
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

  @Test
  public void testMinAccessEgressDurationForMode() {
    // We only need one element since we are testing the "wiring", not the business rules
    Map<String, Object> arguments = Map.of(
      MIN_ACCESS_EGRESS_DURATION_FOR_MODE,
      List.<Map<String, Object>>of(Map.of(FIELD_STREET_MODE, CAR, FIELD_DURATION, D10m))
    );
    var routeRequest = TripRequestMapper.createRequest(executionContext(arguments));
    var durationForMode = routeRequest.preferences().street().minAccessEgressDuration();

    assertEquals(D10m, durationForMode.valueOf(CAR));
    assertEquals(MIN_DEFAULT, durationForMode.valueOf(WALK));
    assertEquals(MIN_FLEXIBLE, durationForMode.valueOf(FLEXIBLE));
  }

  @Test
  public void testMaxAccessEgressDurationForMode() {
    Map<String, Object> arguments = Map.of(
      MAX_ACCESS_EGRESS_DURATION_FOR_MODE,
      List.<Map<String, Object>>of(Map.of(FIELD_STREET_MODE, CAR, FIELD_DURATION, D10m))
    );
    var routeRequest = TripRequestMapper.createRequest(executionContext(arguments));
    var durationForMode = routeRequest.preferences().street().maxAccessEgressDuration();

    assertEquals(D10m, durationForMode.valueOf(CAR));
    assertEquals(MAX_DEFAULT, durationForMode.valueOf(WALK));
    assertEquals(MAX_FLEXIBLE, durationForMode.valueOf(FLEXIBLE));
  }

  @Test
  public void testMaxDirectDurationForMode() {
    Map<String, Object> arguments = Map.of(
      MAX_DIRECT_DURATION_FOR_MODE,
      List.<Map<String, Object>>of(Map.of(FIELD_STREET_MODE, CAR, FIELD_DURATION, D10m))
    );
    var routeRequest = TripRequestMapper.createRequest(executionContext(arguments));
    var durationForMode = routeRequest.preferences().street().maxDirectDuration();

    assertEquals(D10m, durationForMode.valueOf(CAR));
    assertEquals(MAX_DEFAULT, durationForMode.valueOf(WALK));
    assertEquals(MAX_FLEXIBLE, durationForMode.valueOf(FLEXIBLE));
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
