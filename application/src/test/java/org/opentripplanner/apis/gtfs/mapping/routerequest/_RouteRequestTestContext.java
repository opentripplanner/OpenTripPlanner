package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static java.util.Map.entry;
import static org.opentripplanner.apis.gtfs.SchemaObjectMappersForTests.mapCoordinate;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.SchemaFactory;
import org.opentripplanner.apis.gtfs.TestRoutingService;
import org.opentripplanner.ext.fares.impl.gtfs.DefaultFareService;
import org.opentripplanner.graph_builder.module.linking.TestVertexLinker;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;

class _RouteRequestTestContext {

  static final Coordinate ORIGIN = new Coordinate(1.0, 2.0);
  static final Coordinate DESTINATION = new Coordinate(2.0, 1.0);

  static final Map<String, Object> ARGS = Map.ofEntries(
    entry(
      "origin",
      Map.ofEntries(entry("location", Map.of("coordinate", mapCoordinate(ORIGIN.x, ORIGIN.y))))
    ),
    entry(
      "destination",
      Map.ofEntries(
        entry("location", Map.of("coordinate", mapCoordinate(DESTINATION.x, DESTINATION.y)))
      )
    )
  );

  private final GraphQLRequestContext context;
  private final Locale locale;

  public _RouteRequestTestContext(Locale locale) {
    this.locale = locale;

    Graph graph = new Graph();
    var timetableRepository = new TimetableRepository();
    timetableRepository.initTimeZone(ZoneIds.BERLIN);
    final DefaultTransitService transitService = new DefaultTransitService(timetableRepository);
    var routeRequest = RouteRequest.defaultValue();
    this.context = new GraphQLRequestContext(
      new TestRoutingService(List.of()),
      transitService,
      new DefaultFareService(),
      new DefaultVehicleRentalService(),
      new DefaultVehicleParkingService(new DefaultVehicleParkingRepository()),
      new DefaultRealtimeVehicleService(transitService),
      SchemaFactory.createSchemaWithDefaultInjection(routeRequest),
      GraphFinder.getInstance(
        graph,
        TestVertexLinker.of(graph),
        transitService::findRegularStopsByBoundingBox
      ),
      routeRequest
    );
  }

  public Map<String, Object> basicRequest() {
    var newArgs = new HashMap<String, Object>();
    newArgs.putAll(ARGS);
    return newArgs;
  }

  static _RouteRequestTestContext of(Locale locale) {
    return new _RouteRequestTestContext(locale);
  }

  public GraphQLRequestContext context() {
    return context;
  }

  public Locale locale() {
    return locale;
  }

  DataFetchingEnvironment executionContext(Map<String, Object> arguments) {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
      .query("")
      .operationName("planConnection")
      .context(context)
      .locale(locale)
      .build();

    var executionContext = newExecutionContextBuilder()
      .executionInput(executionInput)
      .executionId(ExecutionId.from("planConnectionTest"))
      .build();
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .arguments(arguments)
      .localContext(Map.of("locale", locale))
      .build();
  }
}
