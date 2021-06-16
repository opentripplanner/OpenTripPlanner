package org.opentripplanner.ext.legacygraphqlapi;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.*;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class LegacyGraphQLIndex {

  static final Logger LOG = LoggerFactory.getLogger(LegacyGraphQLIndex.class);

  static private final GraphQLSchema indexSchema = buildSchema();

  static final ExecutorService threadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
      .setNameFormat("GraphQLExecutor-%d")
      .build());

  static private GraphQLSchema buildSchema() {
    try {
      URL url = Resources.getResource("legacygraphqlapi/schema.graphqls");
      String sdl = Resources.toString(url, Charsets.UTF_8);
      TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
      RuntimeWiring runtimeWiring = RuntimeWiring
          .newRuntimeWiring()
          .scalar(LegacyGraphQLScalars.polylineScalar)
          .scalar(LegacyGraphQLScalars.graphQLIDScalar)
          .type("Node", type -> type.typeResolver(new LegacyGraphQLNodeTypeResolver()))
          .type("PlaceInterface", type -> type.typeResolver(new LegacyGraphQLPlaceInterfaceTypeResolver()))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLAgencyImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLAlertImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLBikeParkImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLBikeRentalStationImpl.class))
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
          .build();
      SchemaGenerator schemaGenerator = new SchemaGenerator();
      return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }
    catch (Exception e) {
      LOG.error("Unable to build Legacy GraphQL Schema", e);
    }
    return null;
  }

  static HashMap<String, Object> getGraphQLExecutionResult(
      String query, Router router, Map<String, Object> variables, String operationName,
      int maxResolves, int timeoutMs, Locale locale
  ) {
    MaxQueryComplexityInstrumentation instrumentation = new MaxQueryComplexityInstrumentation(
        maxResolves);
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
    HashMap<String, Object> content = new HashMap<>();
    ExecutionResult executionResult;
    try {
      executionResult = graphQL.executeAsync(executionInput).get(timeoutMs, TimeUnit.MILLISECONDS);
      if (!executionResult.getErrors().isEmpty()) {
        content.put("errors", mapErrors(executionResult.getErrors()));
      }
      if (executionResult.getData() != null) {
        content.put("data", executionResult.getData());
      }
    }
    catch (Exception e) {
      Throwable reason = e;
      if (e.getCause() != null) { reason = e.getCause(); }
      LOG.warn("Exception during graphQL.execute: " + reason.getMessage(), reason);
      content.put("errors", mapErrors(List.of(reason)));
    }
    return content;
  }

  static Response getGraphQLResponse(
      String query, Router router, Map<String, Object> variables, String operationName,
      int maxResolves, int timeoutMs, Locale locale
  ) {
    Response.ResponseBuilder res = Response.status(Response.Status.OK);
    HashMap<String, Object> content = getGraphQLExecutionResult(
        query,
        router,
        variables,
        operationName,
        maxResolves,
        timeoutMs,
        locale
    );
    return res.entity(content).build();
  }

  static private List<Map<String, Object>> mapErrors(Collection<?> errors) {
    return errors.stream().map(e -> {
      HashMap<String, Object> response = new HashMap<>();

      if (e instanceof GraphQLError) {
        GraphQLError graphQLError = (GraphQLError) e;
        response.put("message", graphQLError.getMessage());
        response.put("errorType", graphQLError.getErrorType());
        response.put("locations", graphQLError.getLocations());
        response.put("path", graphQLError.getPath());
      }
      else {
        if (e instanceof Exception) {
          response.put("message", ((Exception) e).getMessage());
        }
        response.put("errorType", e.getClass().getSimpleName());
      }

      return response;
    }).collect(Collectors.toList());
  }

}
