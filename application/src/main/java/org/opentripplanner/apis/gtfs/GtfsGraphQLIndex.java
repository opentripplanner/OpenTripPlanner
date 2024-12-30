package org.opentripplanner.apis.gtfs;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import io.micrometer.core.instrument.Metrics;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opentripplanner.apis.support.graphql.LoggingDataFetcherExceptionHandler;
import org.opentripplanner.ext.actuator.MicrometerGraphQLInstrumentation;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.graphql.GraphQLResponseSerializer;

class GtfsGraphQLIndex {

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
      .newGraphQL(requestContext.schemaService().schema())
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
