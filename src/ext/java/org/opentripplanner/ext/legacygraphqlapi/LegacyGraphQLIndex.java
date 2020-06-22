package org.opentripplanner.ext.legacygraphqlapi;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.relay.Relay;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
          //TODO
          .type("Node", type -> type.typeResolver(node -> null))
          //TODO
          .type("PlaceInterface", type -> type.typeResolver(node -> null))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLAgencyImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLAlertImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLBikeParkImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLBikeRentalStationImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLQueryTypeImpl.class))
          .type(IntrospectionTypeWiring.build(LegacyGraphQLServiceTimeRangeImpl.class))
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
      int maxResolves
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
        .build();
    HashMap<String, Object> content = new HashMap<>();
    ExecutionResult executionResult;
    try {
      executionResult = graphQL.execute(executionInput);
      if (!executionResult.getErrors().isEmpty()) {
        content.put("errors", mapErrors(executionResult.getErrors()));
      }
      if (executionResult.getData() != null) {
        content.put("data", executionResult.getData());
      }
    }
    catch (RuntimeException ge) {
      LOG.warn("Exception during graphQL.execute: " + ge.getMessage(), ge);
      content.put("errors", mapErrors(Arrays.asList(ge)));
    }
    return content;
  }

  static Response getGraphQLResponse(
      String query, Router router, Map<String, Object> variables, String operationName,
      int maxResolves
  ) {
    Response.ResponseBuilder res = Response.status(Response.Status.OK);
    HashMap<String, Object> content = getGraphQLExecutionResult(
        query,
        router,
        variables,
        operationName,
        maxResolves
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
