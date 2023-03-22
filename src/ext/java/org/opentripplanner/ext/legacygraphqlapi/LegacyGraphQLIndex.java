package org.opentripplanner.ext.legacygraphqlapi;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLAgencyImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLAlertEntityTypeResolver;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLAlertImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLBikeParkImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLBikeRentalStationImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLBookingInfoImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLBookingTimeImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLCarParkImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLContactInfoImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLCoordinatesImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLDepartureRowImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLFeedImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLGeometryImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLItineraryImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLLegImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLNodeTypeResolver;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLOpeningHoursImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLPatternImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLPlaceImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLPlaceInterfaceTypeResolver;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLPlanImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLQueryTypeImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLRentalVehicleImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLRentalVehicleTypeImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLRideHailingEstimateImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLRouteImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLRouteTypeImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLRoutingErrorImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLStopGeometriesImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLStopImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLStopOnRouteImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLStopOnTripImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLStopRelationshipImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLStoptimeImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLStoptimesInPatternImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLSystemNoticeImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLTicketTypeImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLTranslatedStringImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLTripImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLUnknownImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLVehicleParkingImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLVehiclePositionImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLVehicleRentalStationImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLdebugOutputImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLelevationProfileComponentImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLfareComponentImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLfareImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLplaceAtDistanceImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLserviceTimeRangeImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLstepImpl;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLstopAtDistanceImpl;
import org.opentripplanner.framework.application.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LegacyGraphQLIndex {

  static final Logger LOG = LoggerFactory.getLogger(LegacyGraphQLIndex.class);

  private static final GraphQLSchema indexSchema = buildSchema();

  static final ExecutorService threadPool = Executors.newCachedThreadPool(
    new ThreadFactoryBuilder().setNameFormat("GraphQLExecutor-%d").build()
  );

  protected static GraphQLSchema buildSchema() {
    try {
      URL url = Resources.getResource("legacygraphqlapi/schema.graphqls");
      String sdl = Resources.toString(url, StandardCharsets.UTF_8);
      TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
      IntrospectionTypeWiring typeWiring = new IntrospectionTypeWiring(typeRegistry);
      RuntimeWiring runtimeWiring = RuntimeWiring
        .newRuntimeWiring()
        .scalar(LegacyGraphQLScalars.durationScalar)
        .scalar(LegacyGraphQLScalars.polylineScalar)
        .scalar(LegacyGraphQLScalars.geoJsonScalar)
        .scalar(LegacyGraphQLScalars.graphQLIDScalar)
        .scalar(ExtendedScalars.GraphQLLong)
        .type("Node", type -> type.typeResolver(new LegacyGraphQLNodeTypeResolver()))
        .type(
          "PlaceInterface",
          type -> type.typeResolver(new LegacyGraphQLPlaceInterfaceTypeResolver())
        )
        .type(typeWiring.build(LegacyGraphQLAgencyImpl.class))
        .type(typeWiring.build(LegacyGraphQLAlertImpl.class))
        .type(typeWiring.build(LegacyGraphQLBikeParkImpl.class))
        .type(typeWiring.build(LegacyGraphQLVehicleParkingImpl.class))
        .type(typeWiring.build(LegacyGraphQLBikeRentalStationImpl.class))
        .type(typeWiring.build(LegacyGraphQLCarParkImpl.class))
        .type(typeWiring.build(LegacyGraphQLCoordinatesImpl.class))
        .type(typeWiring.build(LegacyGraphQLdebugOutputImpl.class))
        .type(typeWiring.build(LegacyGraphQLDepartureRowImpl.class))
        .type(typeWiring.build(LegacyGraphQLelevationProfileComponentImpl.class))
        .type(typeWiring.build(LegacyGraphQLfareComponentImpl.class))
        .type(typeWiring.build(LegacyGraphQLfareImpl.class))
        .type(typeWiring.build(LegacyGraphQLFeedImpl.class))
        .type(typeWiring.build(LegacyGraphQLFeedImpl.class))
        .type(typeWiring.build(LegacyGraphQLGeometryImpl.class))
        .type(typeWiring.build(LegacyGraphQLItineraryImpl.class))
        .type(typeWiring.build(LegacyGraphQLLegImpl.class))
        .type(typeWiring.build(LegacyGraphQLPatternImpl.class))
        .type(typeWiring.build(LegacyGraphQLPlaceImpl.class))
        .type(typeWiring.build(LegacyGraphQLplaceAtDistanceImpl.class))
        .type(typeWiring.build(LegacyGraphQLPlanImpl.class))
        .type(typeWiring.build(LegacyGraphQLQueryTypeImpl.class))
        .type(typeWiring.build(LegacyGraphQLRouteImpl.class))
        .type(typeWiring.build(LegacyGraphQLserviceTimeRangeImpl.class))
        .type(typeWiring.build(LegacyGraphQLstepImpl.class))
        .type(typeWiring.build(LegacyGraphQLStopImpl.class))
        .type(typeWiring.build(LegacyGraphQLstopAtDistanceImpl.class))
        .type(typeWiring.build(LegacyGraphQLStoptimeImpl.class))
        .type(typeWiring.build(LegacyGraphQLStoptimesInPatternImpl.class))
        .type(typeWiring.build(LegacyGraphQLTicketTypeImpl.class))
        .type(typeWiring.build(LegacyGraphQLTranslatedStringImpl.class))
        .type(typeWiring.build(LegacyGraphQLTripImpl.class))
        .type(typeWiring.build(LegacyGraphQLSystemNoticeImpl.class))
        .type(typeWiring.build(LegacyGraphQLContactInfoImpl.class))
        .type(typeWiring.build(LegacyGraphQLBookingTimeImpl.class))
        .type(typeWiring.build(LegacyGraphQLBookingInfoImpl.class))
        .type(typeWiring.build(LegacyGraphQLVehicleRentalStationImpl.class))
        .type(typeWiring.build(LegacyGraphQLRentalVehicleImpl.class))
        .type(typeWiring.build(LegacyGraphQLRentalVehicleTypeImpl.class))
        .type("AlertEntity", type -> type.typeResolver(new LegacyGraphQLAlertEntityTypeResolver()))
        .type(typeWiring.build(LegacyGraphQLStopOnRouteImpl.class))
        .type(typeWiring.build(LegacyGraphQLStopOnTripImpl.class))
        .type(typeWiring.build(LegacyGraphQLUnknownImpl.class))
        .type(typeWiring.build(LegacyGraphQLRouteTypeImpl.class))
        .type(typeWiring.build(LegacyGraphQLRoutingErrorImpl.class))
        .type(typeWiring.build(LegacyGraphQLStopGeometriesImpl.class))
        .type(typeWiring.build(LegacyGraphQLVehiclePositionImpl.class))
        .type(typeWiring.build(LegacyGraphQLStopRelationshipImpl.class))
        .type(typeWiring.build(LegacyGraphQLOpeningHoursImpl.class))
        .type(typeWiring.build(LegacyGraphQLRideHailingEstimateImpl.class))
        .build();
      SchemaGenerator schemaGenerator = new SchemaGenerator();
      return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    } catch (Exception e) {
      LOG.error("Unable to build Legacy GraphQL Schema", e);
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
    LegacyGraphQLRequestContext requestContext
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
    LegacyGraphQLRequestContext requestContext
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
