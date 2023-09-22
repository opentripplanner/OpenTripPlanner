package org.opentripplanner.ext.gtfsgraphqlapi;

import com.google.common.io.Resources;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.micrometer.core.instrument.Metrics;
import jakarta.ws.rs.core.Response;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opentripplanner.api.json.GraphQLResponseSerializer;
import org.opentripplanner.ext.actuator.MicrometerGraphQLInstrumentation;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.AgencyImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.AlertEntityTypeResolver;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.AlertImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.BikeParkImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.BikeRentalStationImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.BookingInfoImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.BookingTimeImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.CarParkImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.ContactInfoImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.CoordinatesImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.CurrencyImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.DefaultFareProductImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.DepartureRowImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.FareProductTypeResolver;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.FareProductUseImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.FeedImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.GeometryImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.ItineraryImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.LegImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.MoneyImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.NodeTypeResolver;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.OpeningHoursImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.PatternImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.PlaceImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.PlaceInterfaceTypeResolver;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.PlanImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.QueryTypeImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.RentalVehicleImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.RentalVehicleTypeImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.RideHailingEstimateImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.RouteImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.RouteTypeImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.RoutingErrorImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.StopGeometriesImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.StopImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.StopOnRouteImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.StopOnTripImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.StopRelationshipImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.StoptimeImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.StoptimesInPatternImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.SystemNoticeImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.TicketTypeImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.TranslatedStringImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.TripImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.TripOccupancyImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.UnknownImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.VehicleParkingImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.VehiclePositionImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.VehicleRentalStationImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.debugOutputImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.elevationProfileComponentImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.fareComponentImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.fareImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.placeAtDistanceImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.serviceTimeRangeImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.stepImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.datafetchers.stopAtDistanceImpl;
import org.opentripplanner.ext.gtfsgraphqlapi.model.StopPosition;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.concurrent.OtpRequestThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GtfsGraphQLIndex {

  static final Logger LOG = LoggerFactory.getLogger(GtfsGraphQLIndex.class);

  private static final GraphQLSchema indexSchema = buildSchema();

  static final ExecutorService threadPool = Executors.newCachedThreadPool(
    OtpRequestThreadFactory.of("gtfs-api-%d")
  );

  protected static GraphQLSchema buildSchema() {
    try {
      URL url = Resources.getResource("gtfsgraphqlapi/schema.graphqls");
      String sdl = Resources.toString(url, StandardCharsets.UTF_8);
      TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
      IntrospectionTypeWiring typeWiring = new IntrospectionTypeWiring(typeRegistry);
      RuntimeWiring runtimeWiring = RuntimeWiring
        .newRuntimeWiring()
        .scalar(GraphQLScalars.durationScalar)
        .scalar(GraphQLScalars.polylineScalar)
        .scalar(GraphQLScalars.geoJsonScalar)
        .scalar(GraphQLScalars.graphQLIDScalar)
        .scalar(ExtendedScalars.GraphQLLong)
        .type("Node", type -> type.typeResolver(new NodeTypeResolver()))
        .type("PlaceInterface", type -> type.typeResolver(new PlaceInterfaceTypeResolver()))
        .type("StopPosition", type -> type.typeResolver(new StopPosition() {}))
        .type("FareProduct", type -> type.typeResolver(new FareProductTypeResolver()))
        .type(typeWiring.build(AgencyImpl.class))
        .type(typeWiring.build(AlertImpl.class))
        .type(typeWiring.build(BikeParkImpl.class))
        .type(typeWiring.build(VehicleParkingImpl.class))
        .type(typeWiring.build(BikeRentalStationImpl.class))
        .type(typeWiring.build(CarParkImpl.class))
        .type(typeWiring.build(CoordinatesImpl.class))
        .type(typeWiring.build(debugOutputImpl.class))
        .type(typeWiring.build(DepartureRowImpl.class))
        .type(typeWiring.build(elevationProfileComponentImpl.class))
        .type(typeWiring.build(fareComponentImpl.class))
        .type(typeWiring.build(fareImpl.class))
        .type(typeWiring.build(FeedImpl.class))
        .type(typeWiring.build(FeedImpl.class))
        .type(typeWiring.build(GeometryImpl.class))
        .type(typeWiring.build(ItineraryImpl.class))
        .type(typeWiring.build(LegImpl.class))
        .type(typeWiring.build(PatternImpl.class))
        .type(typeWiring.build(PlaceImpl.class))
        .type(typeWiring.build(placeAtDistanceImpl.class))
        .type(typeWiring.build(PlanImpl.class))
        .type(typeWiring.build(QueryTypeImpl.class))
        .type(typeWiring.build(RouteImpl.class))
        .type(typeWiring.build(serviceTimeRangeImpl.class))
        .type(typeWiring.build(stepImpl.class))
        .type(typeWiring.build(StopImpl.class))
        .type(typeWiring.build(stopAtDistanceImpl.class))
        .type(typeWiring.build(StoptimeImpl.class))
        .type(typeWiring.build(StoptimesInPatternImpl.class))
        .type(typeWiring.build(TicketTypeImpl.class))
        .type(typeWiring.build(TranslatedStringImpl.class))
        .type(typeWiring.build(TripImpl.class))
        .type(typeWiring.build(SystemNoticeImpl.class))
        .type(typeWiring.build(ContactInfoImpl.class))
        .type(typeWiring.build(BookingTimeImpl.class))
        .type(typeWiring.build(BookingInfoImpl.class))
        .type(typeWiring.build(VehicleRentalStationImpl.class))
        .type(typeWiring.build(RentalVehicleImpl.class))
        .type(typeWiring.build(RentalVehicleTypeImpl.class))
        .type("AlertEntity", type -> type.typeResolver(new AlertEntityTypeResolver()))
        .type(typeWiring.build(StopOnRouteImpl.class))
        .type(typeWiring.build(StopOnTripImpl.class))
        .type(typeWiring.build(UnknownImpl.class))
        .type(typeWiring.build(RouteTypeImpl.class))
        .type(typeWiring.build(RoutingErrorImpl.class))
        .type(typeWiring.build(StopGeometriesImpl.class))
        .type(typeWiring.build(VehiclePositionImpl.class))
        .type(typeWiring.build(StopRelationshipImpl.class))
        .type(typeWiring.build(OpeningHoursImpl.class))
        .type(typeWiring.build(RideHailingEstimateImpl.class))
        .type(typeWiring.build(MoneyImpl.class))
        .type(typeWiring.build(CurrencyImpl.class))
        .type(typeWiring.build(FareProductUseImpl.class))
        .type(typeWiring.build(DefaultFareProductImpl.class))
        .type(typeWiring.build(TripOccupancyImpl.class))
        .build();
      SchemaGenerator schemaGenerator = new SchemaGenerator();
      return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    } catch (Exception e) {
      LOG.error("Unable to build GTFS GraphQL Schema", e);
    }
    return null;
  }

  static ExecutionResult getGraphQLExecutionResult(
    String query,
    Map<String, Object> variables,
    String operationName,
    int maxResolves,
    int timeoutMs,
    Locale locale,
    GraphQLRequestContext requestContext
  ) {
    Instrumentation instrumentation = new MaxQueryComplexityInstrumentation(maxResolves);

    if (OTPFeature.ActuatorAPI.isOn()) {
      instrumentation =
        new ChainedInstrumentation(
          new MicrometerGraphQLInstrumentation(Metrics.globalRegistry, List.of()),
          instrumentation
        );
    }

    GraphQL graphQL = GraphQL.newGraphQL(indexSchema).instrumentation(instrumentation).build();

    if (variables == null) {
      variables = new HashMap<>();
    }

    ExecutionInput executionInput = ExecutionInput
      .newExecutionInput()
      .query(query)
      .operationName(operationName)
      .context(requestContext)
      .variables(variables)
      .locale(locale)
      .build();
    try {
      return graphQL.executeAsync(executionInput).get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      return new AbortExecutionException(e).toExecutionResult();
    }
  }

  static Response getGraphQLResponse(
    String query,
    Map<String, Object> variables,
    String operationName,
    int maxResolves,
    int timeoutMs,
    Locale locale,
    GraphQLRequestContext requestContext
  ) {
    ExecutionResult executionResult = getGraphQLExecutionResult(
      query,
      variables,
      operationName,
      maxResolves,
      timeoutMs,
      locale,
      requestContext
    );

    return Response
      .status(Response.Status.OK)
      .entity(GraphQLResponseSerializer.serialize(executionResult))
      .build();
  }
}
