package org.opentripplanner.apis.gtfs;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opentripplanner.apis.gtfs.datafetchers.AgencyImpl;
import org.opentripplanner.apis.gtfs.datafetchers.AlertEntityTypeResolver;
import org.opentripplanner.apis.gtfs.datafetchers.AlertImpl;
import org.opentripplanner.apis.gtfs.datafetchers.BikeParkImpl;
import org.opentripplanner.apis.gtfs.datafetchers.BikeRentalStationImpl;
import org.opentripplanner.apis.gtfs.datafetchers.BookingInfoImpl;
import org.opentripplanner.apis.gtfs.datafetchers.BookingTimeImpl;
import org.opentripplanner.apis.gtfs.datafetchers.CallScheduledTimeTypeResolver;
import org.opentripplanner.apis.gtfs.datafetchers.CallStopLocationTypeResolver;
import org.opentripplanner.apis.gtfs.datafetchers.CarParkImpl;
import org.opentripplanner.apis.gtfs.datafetchers.ContactInfoImpl;
import org.opentripplanner.apis.gtfs.datafetchers.CoordinatesImpl;
import org.opentripplanner.apis.gtfs.datafetchers.CurrencyImpl;
import org.opentripplanner.apis.gtfs.datafetchers.DefaultFareProductImpl;
import org.opentripplanner.apis.gtfs.datafetchers.DepartureRowImpl;
import org.opentripplanner.apis.gtfs.datafetchers.EntranceImpl;
import org.opentripplanner.apis.gtfs.datafetchers.EstimatedTimeImpl;
import org.opentripplanner.apis.gtfs.datafetchers.FareProductTypeResolver;
import org.opentripplanner.apis.gtfs.datafetchers.FareProductUseImpl;
import org.opentripplanner.apis.gtfs.datafetchers.FeedImpl;
import org.opentripplanner.apis.gtfs.datafetchers.GeometryImpl;
import org.opentripplanner.apis.gtfs.datafetchers.ItineraryImpl;
import org.opentripplanner.apis.gtfs.datafetchers.LegImpl;
import org.opentripplanner.apis.gtfs.datafetchers.LegTimeImpl;
import org.opentripplanner.apis.gtfs.datafetchers.MoneyImpl;
import org.opentripplanner.apis.gtfs.datafetchers.NodeTypeResolver;
import org.opentripplanner.apis.gtfs.datafetchers.OpeningHoursImpl;
import org.opentripplanner.apis.gtfs.datafetchers.PatternImpl;
import org.opentripplanner.apis.gtfs.datafetchers.PlaceImpl;
import org.opentripplanner.apis.gtfs.datafetchers.PlaceInterfaceTypeResolver;
import org.opentripplanner.apis.gtfs.datafetchers.PlanConnectionImpl;
import org.opentripplanner.apis.gtfs.datafetchers.PlanImpl;
import org.opentripplanner.apis.gtfs.datafetchers.QueryTypeImpl;
import org.opentripplanner.apis.gtfs.datafetchers.RealTimeEstimateImpl;
import org.opentripplanner.apis.gtfs.datafetchers.RentalPlaceTypeResolver;
import org.opentripplanner.apis.gtfs.datafetchers.RentalVehicleFuelImpl;
import org.opentripplanner.apis.gtfs.datafetchers.RentalVehicleImpl;
import org.opentripplanner.apis.gtfs.datafetchers.RentalVehicleTypeImpl;
import org.opentripplanner.apis.gtfs.datafetchers.RideHailingEstimateImpl;
import org.opentripplanner.apis.gtfs.datafetchers.RouteImpl;
import org.opentripplanner.apis.gtfs.datafetchers.RouteTypeImpl;
import org.opentripplanner.apis.gtfs.datafetchers.RoutingErrorImpl;
import org.opentripplanner.apis.gtfs.datafetchers.StepFeatureTypeResolver;
import org.opentripplanner.apis.gtfs.datafetchers.StopCallImpl;
import org.opentripplanner.apis.gtfs.datafetchers.StopGeometriesImpl;
import org.opentripplanner.apis.gtfs.datafetchers.StopImpl;
import org.opentripplanner.apis.gtfs.datafetchers.StopOnRouteImpl;
import org.opentripplanner.apis.gtfs.datafetchers.StopOnTripImpl;
import org.opentripplanner.apis.gtfs.datafetchers.StopRelationshipImpl;
import org.opentripplanner.apis.gtfs.datafetchers.StoptimeImpl;
import org.opentripplanner.apis.gtfs.datafetchers.StoptimesInPatternImpl;
import org.opentripplanner.apis.gtfs.datafetchers.SystemNoticeImpl;
import org.opentripplanner.apis.gtfs.datafetchers.TicketTypeImpl;
import org.opentripplanner.apis.gtfs.datafetchers.TranslatedStringImpl;
import org.opentripplanner.apis.gtfs.datafetchers.TripImpl;
import org.opentripplanner.apis.gtfs.datafetchers.TripOccupancyImpl;
import org.opentripplanner.apis.gtfs.datafetchers.TripOnServiceDateImpl;
import org.opentripplanner.apis.gtfs.datafetchers.UnknownImpl;
import org.opentripplanner.apis.gtfs.datafetchers.VehicleParkingImpl;
import org.opentripplanner.apis.gtfs.datafetchers.VehiclePositionImpl;
import org.opentripplanner.apis.gtfs.datafetchers.VehicleRentalNetworkImpl;
import org.opentripplanner.apis.gtfs.datafetchers.VehicleRentalStationImpl;
import org.opentripplanner.apis.gtfs.datafetchers.debugOutputImpl;
import org.opentripplanner.apis.gtfs.datafetchers.elevationProfileComponentImpl;
import org.opentripplanner.apis.gtfs.datafetchers.placeAtDistanceImpl;
import org.opentripplanner.apis.gtfs.datafetchers.serviceTimeRangeImpl;
import org.opentripplanner.apis.gtfs.datafetchers.stepImpl;
import org.opentripplanner.apis.gtfs.datafetchers.stopAtDistanceImpl;
import org.opentripplanner.apis.gtfs.model.StopPosition;
import org.opentripplanner.apis.support.graphql.LoggingDataFetcherExceptionHandler;
import org.opentripplanner.ext.actuator.MicrometerGraphQLInstrumentation;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.graphql.GraphQLResponseSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GtfsGraphQLIndex {

  static final Logger LOG = LoggerFactory.getLogger(GtfsGraphQLIndex.class);

  private static final GraphQLSchema indexSchema = buildSchema();

  protected static GraphQLSchema buildSchema() {
    try {
      URL url = Objects.requireNonNull(GtfsGraphQLIndex.class.getResource("schema.graphqls"));
      TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(url.openStream());
      IntrospectionTypeWiring typeWiring = new IntrospectionTypeWiring(typeRegistry);
      RuntimeWiring runtimeWiring = RuntimeWiring
        .newRuntimeWiring()
        .scalar(GraphQLScalars.DURATION_SCALAR)
        .scalar(GraphQLScalars.POLYLINE_SCALAR)
        .scalar(GraphQLScalars.GEOJSON_SCALAR)
        .scalar(GraphQLScalars.GRAPHQL_ID_SCALAR)
        .scalar(GraphQLScalars.GRAMS_SCALAR)
        .scalar(GraphQLScalars.OFFSET_DATETIME_SCALAR)
        .scalar(GraphQLScalars.RATIO_SCALAR)
        .scalar(GraphQLScalars.COORDINATE_VALUE_SCALAR)
        .scalar(GraphQLScalars.COST_SCALAR)
        .scalar(GraphQLScalars.RELUCTANCE_SCALAR)
        .scalar(GraphQLScalars.LOCAL_DATE_SCALAR)
        .scalar(ExtendedScalars.GraphQLLong)
        .scalar(ExtendedScalars.Locale)
        .scalar(
          ExtendedScalars
            .newAliasedScalar("Speed")
            .aliasedScalar(ExtendedScalars.NonNegativeFloat)
            .build()
        )
        .type("Node", type -> type.typeResolver(new NodeTypeResolver()))
        .type("PlaceInterface", type -> type.typeResolver(new PlaceInterfaceTypeResolver()))
        .type("RentalPlace", type -> type.typeResolver(new RentalPlaceTypeResolver()))
        .type("StopPosition", type -> type.typeResolver(new StopPosition() {}))
        .type("FareProduct", type -> type.typeResolver(new FareProductTypeResolver()))
        .type("AlertEntity", type -> type.typeResolver(new AlertEntityTypeResolver()))
        .type("CallStopLocation", type -> type.typeResolver(new CallStopLocationTypeResolver()))
        .type("CallScheduledTime", type -> type.typeResolver(new CallScheduledTimeTypeResolver()))
        .type("StepFeature", type -> type.typeResolver(new StepFeatureTypeResolver()))
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
        .type(typeWiring.build(FeedImpl.class))
        .type(typeWiring.build(GeometryImpl.class))
        .type(typeWiring.build(ItineraryImpl.class))
        .type(typeWiring.build(LegImpl.class))
        .type(typeWiring.build(PatternImpl.class))
        .type(typeWiring.build(PlaceImpl.class))
        .type(typeWiring.build(placeAtDistanceImpl.class))
        .type(typeWiring.build(PlanConnectionImpl.class))
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
        .type(typeWiring.build(VehicleRentalNetworkImpl.class))
        .type(typeWiring.build(RentalVehicleImpl.class))
        .type(typeWiring.build(RentalVehicleTypeImpl.class))
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
        .type(typeWiring.build(TripOnServiceDateImpl.class))
        .type(typeWiring.build(StopCallImpl.class))
        .type(typeWiring.build(TripOccupancyImpl.class))
        .type(typeWiring.build(LegTimeImpl.class))
        .type(typeWiring.build(RealTimeEstimateImpl.class))
        .type(typeWiring.build(EstimatedTimeImpl.class))
        .type(typeWiring.build(EntranceImpl.class))
        .type(typeWiring.build(RentalVehicleFuelImpl.class))
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

    GraphQL graphQL = GraphQL
      .newGraphQL(indexSchema)
      .instrumentation(instrumentation)
      .defaultDataFetcherExceptionHandler(new LoggingDataFetcherExceptionHandler())
      .build();

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
