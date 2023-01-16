package org.opentripplanner.ext.transmodelapi;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLSchema;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opentripplanner.api.json.GraphQLResponseSerializer;
import org.opentripplanner.ext.actuator.MicrometerGraphQLInstrumentation;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TransmodelGraph {

  static final Logger LOG = LoggerFactory.getLogger(TransmodelGraph.class);

  private final GraphQLSchema indexSchema;

  final ExecutorService threadPool;

  TransmodelGraph(GraphQLSchema schema) {
    this.threadPool =
      Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setNameFormat("GraphQLExecutor-%d").build()
      );
    this.indexSchema = schema;
  }

  ExecutionResult getGraphQLExecutionResult(
    String query,
    OtpServerRequestContext serverContext,
    Map<String, Object> variables,
    String operationName,
    int maxResolves,
    Iterable<Tag> tracingTags
  ) {
    Instrumentation instrumentation = new MaxQueryComplexityInstrumentation(maxResolves);
    if (OTPFeature.ActuatorAPI.isOn()) {
      instrumentation =
        new ChainedInstrumentation(
          new MicrometerGraphQLInstrumentation(Metrics.globalRegistry, tracingTags),
          instrumentation
        );
    }

    GraphQL graphQL = GraphQL.newGraphQL(indexSchema).instrumentation(instrumentation).build();

    if (variables == null) {
      variables = new HashMap<>();
    }

    TransmodelRequestContext transmodelRequestContext = new TransmodelRequestContext(
      serverContext,
      serverContext.routingService(),
      serverContext.transitService()
    );

    ExecutionInput executionInput = ExecutionInput
      .newExecutionInput()
      .query(query)
      .operationName(operationName)
      .context(transmodelRequestContext)
      .root(serverContext)
      .variables(variables)
      .build();
    return graphQL.execute(executionInput);
  }

  Response getGraphQLResponse(
    String query,
    OtpServerRequestContext serverContext,
    Map<String, Object> variables,
    String operationName,
    int maxResolves,
    Iterable<Tag> tracingTags
  ) {
    ExecutionResult result = getGraphQLExecutionResult(
      query,
      serverContext,
      variables,
      operationName,
      maxResolves,
      tracingTags
    );

    return Response
      .status(Response.Status.OK)
      .entity(GraphQLResponseSerializer.serialize(result))
      .build();
  }
}
