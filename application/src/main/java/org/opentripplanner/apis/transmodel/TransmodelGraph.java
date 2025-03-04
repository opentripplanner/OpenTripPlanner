package org.opentripplanner.apis.transmodel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutionStrategy;
import graphql.execution.UnknownOperationException;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLSchema;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opentripplanner.apis.support.graphql.LoggingDataFetcherExceptionHandler;
import org.opentripplanner.apis.transmodel.support.AbortOnUnprocessableRequestExecutionStrategy;
import org.opentripplanner.apis.transmodel.support.ExecutionResultMapper;
import org.opentripplanner.ext.actuator.MicrometerGraphQLInstrumentation;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.concurrent.OtpRequestThreadFactory;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.opentripplanner.utils.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TransmodelGraph {

  private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraph.class);

  private static final int MAX_ERROR_TO_RETURN = 25;
  private final GraphQLSchema indexSchema;

  final ExecutorService threadPool;

  TransmodelGraph(GraphQLSchema schema) {
    this.threadPool = Executors.newCachedThreadPool(
      OtpRequestThreadFactory.of("transmodel-api-%d")
    );
    this.indexSchema = schema;
  }

  Response executeGraphQL(
    String query,
    OtpServerRequestContext serverContext,
    Map<String, Object> variables,
    String operationName,
    int maxNumberOfResultFields,
    Iterable<Tag> tracingTags
  ) {
    try (var executionStrategy = new AbortOnUnprocessableRequestExecutionStrategy()) {
      variables = ObjectUtils.ifNotNull(variables, new HashMap<>());
      var instrumentation = createInstrumentation(maxNumberOfResultFields, tracingTags);
      var transmodelRequestContext = createRequestContext(serverContext);
      var executionInput = createExecutionInput(
        query,
        serverContext,
        variables,
        operationName,
        transmodelRequestContext
      );
      var graphQL = createGraphQL(instrumentation, executionStrategy);

      var result = graphQL.execute(executionInput);
      result = limitMaxNumberOfErrors(result);

      return ExecutionResultMapper.okResponse(result);
    } catch (OTPRequestTimeoutException te) {
      return ExecutionResultMapper.timeoutResponse();
    } catch (ResponseTooLargeException rtle) {
      return ExecutionResultMapper.tooLargeResponse(rtle.getMessage());
    } catch (EntityNotFoundException | CoercingParseValueException | UnknownOperationException e) {
      return ExecutionResultMapper.badRequestResponse(e.getMessage());
    } catch (Exception systemError) {
      LOG.error(systemError.getMessage(), systemError);
      return ExecutionResultMapper.systemErrorResponse(systemError.getMessage());
    }
  }

  private static Instrumentation createInstrumentation(
    int maxNumberOfResultFields,
    Iterable<Tag> tracingTags
  ) {
    Instrumentation instrumentation = new MaxFieldsInResultInstrumentation(maxNumberOfResultFields);

    if (OTPFeature.ActuatorAPI.isOn()) {
      instrumentation = new ChainedInstrumentation(
        new MicrometerGraphQLInstrumentation(Metrics.globalRegistry, tracingTags),
        instrumentation
      );
    }
    return instrumentation;
  }

  private static TransmodelRequestContext createRequestContext(
    OtpServerRequestContext serverContext
  ) {
    return new TransmodelRequestContext(
      serverContext,
      serverContext.routingService(),
      serverContext.transitService()
    );
  }

  private static ExecutionInput createExecutionInput(
    String query,
    OtpServerRequestContext serverContext,
    Map<String, Object> variables,
    String operationName,
    TransmodelRequestContext transmodelRequestContext
  ) {
    return ExecutionInput.newExecutionInput()
      .query(query)
      .operationName(operationName)
      .context(transmodelRequestContext)
      .root(serverContext)
      .variables(variables)
      .build();
  }

  private GraphQL createGraphQL(
    Instrumentation instrumentation,
    ExecutionStrategy executionStrategy
  ) {
    return GraphQL.newGraphQL(indexSchema)
      .instrumentation(instrumentation)
      .queryExecutionStrategy(executionStrategy)
      .defaultDataFetcherExceptionHandler(new LoggingDataFetcherExceptionHandler())
      .build();
  }

  /**
   * Reduce the number of errors returned down to limit
   */
  private static ExecutionResult limitMaxNumberOfErrors(ExecutionResult result) {
    var errors = result.getErrors();
    if (errors.size() > MAX_ERROR_TO_RETURN) {
      final var errorsShortList = errors.stream().limit(MAX_ERROR_TO_RETURN).toList();
      result = result.transform(b -> b.errors(errorsShortList));
    }
    return result;
  }
}
