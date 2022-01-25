package org.opentripplanner.ext.legacygraphqlapi;

import com.google.api.client.util.Charsets;
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
import java.util.List;
import org.opentripplanner.api.json.GraphQLResponseSerializer;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.*;
import org.opentripplanner.ext.actuator.ActuatorAPI;
import org.opentripplanner.ext.actuator.MicrometerGraphQLInstrumentation;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class LegacyGraphQLIndex {

  static final Logger LOG = LoggerFactory.getLogger(LegacyGraphQLIndex.class);

  static private final GraphQLSchema indexSchema = buildSchema();

  static final ExecutorService threadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
      .setNameFormat("GraphQLExecutor-%d")
      .build());

  static protected GraphQLSchema buildSchema() {
    try {
      URL url = Resources.getResource("legacygraphqlapi/schema.graphqls");
      String sdl = Resources.toString(url, Charsets.UTF_8);
      TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
      RuntimeWiring runtimeWiring = RuntimeWiring
          .newRuntimeWiring()
          .scalar(LegacyGraphQLScalars.polylineScalar)
          .scalar(LegacyGraphQLScalars.geoJsonScalar)
          .scalar(LegacyGraphQLScalars.graphQLIDScalar)
          .scalar(ExtendedScalars.GraphQLLong)
          .type("Node", type -> type.typeResolver(new LegacyGraphQLNodeTypeResolver()))
          .type("PlaceInterface", type -> type.typeResolver(new LegacyGraphQLPlaceInterfaceTypeResolver()))
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
          .type(IntrospectionTypeWiring.build(LegacyGraphQLTranslatedStringImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLTripImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLSystemNoticeImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLContactInfoImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLBookingTimeImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLBookingInfoImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLVehicleRentalStationImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLRentalVehicleImpl.class))
          .type("AlertEntity", type -> type.typeResolver(new LegacyGraphQLAlertEntityTypeResolver()))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLStopOnRouteImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLStopOnTripImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLUnknownImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLRouteTypeImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLStopGeometriesImpl.class))
          .build();
      SchemaGenerator schemaGenerator = new SchemaGenerator();
      return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }
    catch (Exception e) {
      LOG.error("Unable to build Legacy GraphQL Schema", e);
    }
    return null;
  }

  static ExecutionResult getGraphQLExecutionResult(
      String query, Router router, Map<String, Object> variables, String operationName,
      int maxResolves, int timeoutMs, Locale locale
  ) {
    Instrumentation instrumentation = new MaxQueryComplexityInstrumentation(
        maxResolves);

    if (OTPFeature.ActuatorAPI.isOn()) {
      instrumentation = new ChainedInstrumentation(
              new MicrometerGraphQLInstrumentation(Metrics.globalRegistry, List.of()),
              instrumentation
      );
    }

    GraphQL graphQL = GraphQL.newGraphQL(indexSchema).instrumentation(instrumentation).build();

    if (variables == null) {
      variables = new HashMap<>();
    }

    LegacyGraphQLRequestContext requestContext = new LegacyGraphQLRequestContext(
        router,
        new RoutingService(router.graph)
    );

    ExecutionInput executionInput = ExecutionInput
        .newExecutionInput()
        .query(query)
        .operationName(operationName)
        .context(requestContext)
        .root(router)
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
      String query, Router router, Map<String, Object> variables, String operationName,
      int maxResolves, int timeoutMs, Locale locale
  ) {
    ExecutionResult executionResult = getGraphQLExecutionResult(
        query,
        router,
        variables,
        operationName,
        maxResolves,
        timeoutMs,
        locale
    );

    return Response.status(Response.Status.OK).entity(GraphQLResponseSerializer.serialize(executionResult)).build();
  }
}
