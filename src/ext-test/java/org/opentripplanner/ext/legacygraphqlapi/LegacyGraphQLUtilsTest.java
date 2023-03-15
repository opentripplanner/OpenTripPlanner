package org.opentripplanner.ext.legacygraphqlapi;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import graphql.ExecutionInput;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class LegacyGraphQLUtilsTest {

  static final LegacyGraphQLRequestContext context;

  static {
    Graph graph = new Graph();
    var transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.BERLIN);
    final DefaultTransitService transitService = new DefaultTransitService(transitModel);
    context =
      new LegacyGraphQLRequestContext(
        new TestRoutingService(List.of()),
        transitService,
        new DefaultFareService(),
        graph.getVehicleParkingService(),
        new DefaultVehicleRentalService(),
        new DefaultVehiclePositionService(),
        GraphFinder.getInstance(graph, transitService::findRegularStop),
        new RouteRequest()
      );
  }

  @Test
  void testGetLocaleWithDefinedLocaleArg() {
    var executionContext = getGenericExecutionContext();

    var env = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .localContext(Map.of("locale", Locale.GERMAN))
      .locale(Locale.ENGLISH)
      .build();

    var frenchLocale = Locale.FRENCH;

    var locale = LegacyGraphQLUtils.getLocale(env, frenchLocale.toString());

    assertEquals(frenchLocale, locale);
  }

  @Test
  void testGetLocaleWithEnvLocale() {
    var executionContext = getGenericExecutionContext();

    var frenchLocale = Locale.FRENCH;
    var env = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .localContext(Map.of("locale", Locale.GERMAN))
      .locale(frenchLocale)
      .build();

    var locale = LegacyGraphQLUtils.getLocale(env);

    assertEquals(frenchLocale, locale);
  }

  @Test
  void testGetLocaleWithLocalContextLocale() {
    var executionContext = getGenericExecutionContext();

    // Should use locale from local context if env locale is not defined

    var frenchLocale = Locale.FRENCH;
    var envWithNoLocale = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .localContext(Map.of("locale", Locale.FRENCH))
      .build();

    var locale = LegacyGraphQLUtils.getLocale(envWithNoLocale);

    assertEquals(frenchLocale, locale);

    // Wildcard locale from env should not override locale from local context if it's defined

    var wildcardLocale = new Locale("*");

    var envWithWildcardLocale = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .locale(wildcardLocale)
      .localContext(Map.of("locale", Locale.FRENCH))
      .build();

    locale = LegacyGraphQLUtils.getLocale(envWithWildcardLocale);

    assertEquals(frenchLocale, locale);
  }

  private ExecutionContext getGenericExecutionContext() {
    ExecutionInput executionInput = ExecutionInput
      .newExecutionInput()
      .query("")
      .operationName("plan")
      .context(context)
      .locale(Locale.ENGLISH)
      .build();

    return newExecutionContextBuilder()
      .executionInput(executionInput)
      .executionId(ExecutionId.from(this.getClass().getName()))
      .build();
  }
}
