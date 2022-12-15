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
import javax.ws.rs.core.Response;
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
import org.opentripplanner.standalone.api.OtpServerRequestContext;
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
      RuntimeWiring runtimeWiring = RuntimeWiring
        .newRuntimeWiring()
        .scalar(LegacyGraphQLScalars.polylineScalar)
        .scalar(LegacyGraphQLScalars.geoJsonScalar)
        .scalar(LegacyGraphQLScalars.graphQLIDScalar)
        .scalar(ExtendedScalars.GraphQLLong)
        .type("Node", type -> type.typeResolver(new LegacyGraphQLNodeTypeResolver()))
        .type(
          "PlaceInterface",
          type -> type.typeResolver(new LegacyGraphQLPlaceInterfaceTypeResolver())
        )
        .type(IntrospectionTypeWiring.build(LegacyGraphQLAgencyImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLAlertImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLBikeParkImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLVehicleParkingImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLBikeRentalStationImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLCarParkImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLCoordinatesImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLdebugOutputImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLDepartureRowImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLelevationProfileComponentImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLfareComponentImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLfareImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLFeedImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLFeedImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLGeometryImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLItineraryImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLLegImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLPatternImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLPlaceImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLplaceAtDistanceImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLPlanImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLQueryTypeImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLRouteImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLserviceTimeRangeImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLstepImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLStopImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLstopAtDistanceImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLStoptimeImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLStoptimesInPatternImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLTicketTypeImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLTranslatedStringImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLTripImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLSystemNoticeImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLContactInfoImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLBookingTimeImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLBookingInfoImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLVehicleRentalStationImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLRentalVehicleImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLRentalVehicleTypeImpl.class))
        .type("AlertEntity", type -> type.typeResolver(new LegacyGraphQLAlertEntityTypeResolver()))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLStopOnRouteImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLStopOnTripImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLUnknownImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLRouteTypeImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLRoutingErrorImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLStopGeometriesImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLVehiclePositionImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLStopRelationshipImpl.class))
        .type(IntrospectionTypeWiring.build(LegacyGraphQLOpeningHoursImpl.class))
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
    OtpServerRequestContext serverContext,
    Map<String, Object> variables,
    String operationName,
    int maxResolves,
    int timeoutMs,
    Locale locale
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

    LegacyGraphQLRequestContext requestContext = new LegacyGraphQLRequestContext(
      serverContext,
      serverContext.routingService(),
      serverContext.transitService(),
      serverContext.graph().getFareService()
    );

    ExecutionInput executionInput = ExecutionInput
      .newExecutionInput()
      .query(query)
      .operationName(operationName)
      .context(requestContext)
      .root(serverContext)
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
    OtpServerRequestContext serverContext,
    Map<String, Object> variables,
    String operationName,
    int maxResolves,
    int timeoutMs,
    Locale locale
  ) {
    ExecutionResult executionResult = getGraphQLExecutionResult(
      query,
      serverContext,
      variables,
      operationName,
      maxResolves,
      timeoutMs,
      locale
    );

    return Response
      .status(Response.Status.OK)
      .entity(GraphQLResponseSerializer.serialize(executionResult))
      .build();
  }
}
