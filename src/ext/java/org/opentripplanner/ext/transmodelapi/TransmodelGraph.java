package org.opentripplanner.ext.transmodelapi;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLSchema;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opentripplanner.ext.actuator.MicrometerGraphQLInstrumentation;
import org.opentripplanner.ext.transmodelapi.support.AbortOnTimeoutExecutionStrategy;
import org.opentripplanner.ext.transmodelapi.support.OtpExecutionResult;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.concurrent.OtpRequestThreadFactory;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

class TransmodelGraph {

  private static final int MAX_ERROR_TO_RETURN = 25;
  private final GraphQLSchema indexSchema;

  final ExecutorService threadPool;

  TransmodelGraph(GraphQLSchema schema) {
    this.threadPool =
      Executors.newCachedThreadPool(OtpRequestThreadFactory.of("transmodel-api-%d"));
    this.indexSchema = schema;
  }

  OtpExecutionResult executeGraphQL(
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

    var executionStrategy = new AbortOnTimeoutExecutionStrategy();
    GraphQL graphQL = GraphQL
      .newGraphQL(indexSchema)
      .instrumentation(instrumentation)
      .queryExecutionStrategy(executionStrategy)
      .build();

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

    // EXECUTE GRAPHQL QUERY
    try {
      var result = graphQL.execute(executionInput);
      result = limitMaxNumberOfErrors(result);
      return OtpExecutionResult.of(result);
    } catch (OTPRequestTimeoutException te) {
      return OtpExecutionResult.ofTimeout();
    } finally {
      executionStrategy.tearDown();
    }
  }

  /**
   * Reduce the number of errors returned down to limit
   */
  private ExecutionResult limitMaxNumberOfErrors(ExecutionResult result) {
    var errors = result.getErrors();
    if (errors.size() > MAX_ERROR_TO_RETURN) {
      final var errorsShortList = errors.stream().limit(MAX_ERROR_TO_RETURN).toList();
      result = result.transform(b -> b.errors(errorsShortList));
    }
    return result;
  }
}
