package org.opentripplanner.ext.actuator;

import static graphql.execution.instrumentation.SimpleInstrumentationContext.noOp;
import static graphql.execution.instrumentation.SimpleInstrumentationContext.whenCompleted;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.ExecutionStrategyInstrumentationContext;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters;
import graphql.language.Document;
import graphql.schema.GraphQLTypeUtil;
import graphql.validation.ValidationError;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.List;

/**
 * Using this instrumentation we can precisely measure how queries and data fetchers are executed
 * and export the metrics to [micrometer](https://micrometer.io).
 * <p>
 * There are two types of metrics: one for query execution, and another for resolver timing. The
 * timers are registered to micrometer using graphql.timer.query and graphql.timer.resolver.
 * <p>
 * ### See also: - https://github.com/symbaloo/graphql-micrometer/blob/main/src/main/kotlin/com/symbaloo/graphqlmicrometer/MicrometerInstrumentation.kt
 * - https://github.com/graphql-java-kickstart/graphql-spring-boot/blob/master/graphql-spring-boot-autoconfigure/src/main/java/graphql/kickstart/autoconfigure/web/servlet/metrics/MetricsInstrumentation.java
 * - https://github.com/apollographql/apollo-tracing - [TracingInstrumentation]
 */
public class MicrometerGraphQLInstrumentation implements Instrumentation {

  private static final String QUERY_TIME_METRIC_NAME = "graphql.timer.query";
  private static final String RESOLVER_TIME_METRIC_NAME = "graphql.timer.resolver";
  private static final String OPERATION_NAME_TAG = "operationName";
  private static final String OPERATION = "operation";
  private static final String PARENT = "parent";
  private static final String FIELD = "field";
  private static final String TIMER_DESCRIPTION =
    "Timer that records the time to fetch the data by Operation Name";

  private final MeterRegistry meterRegistry;
  private final Iterable<Tag> tags;

  public MicrometerGraphQLInstrumentation(MeterRegistry meterRegistry, Iterable<Tag> tags) {
    this.meterRegistry = meterRegistry;
    this.tags = tags;
  }

  @Override
  public InstrumentationState createState(InstrumentationCreateStateParameters parameters) {
    return new TraceState(parameters.getExecutionInput().getOperationName());
  }

  @Override
  public InstrumentationContext<ExecutionResult> beginExecution(
    InstrumentationExecutionParameters parameters,
    InstrumentationState state
  ) {
    Timer.Sample sample = Timer.start(meterRegistry);
    return whenCompleted((res, err) ->
      sample.stop(buildQueryTimer(((TraceState) state).operationName, "execution"))
    );
  }

  @Override
  public InstrumentationContext<Document> beginParse(
    InstrumentationExecutionParameters parameters,
    InstrumentationState state
  ) {
    Timer.Sample sample = Timer.start(meterRegistry);
    return whenCompleted((res, err) ->
      sample.stop(buildQueryTimer(((TraceState) state).operationName, "parse"))
    );
  }

  @Override
  public InstrumentationContext<List<ValidationError>> beginValidation(
    InstrumentationValidationParameters parameters,
    InstrumentationState state
  ) {
    Timer.Sample sample = Timer.start(meterRegistry);
    return whenCompleted((res, err) ->
      sample.stop(buildQueryTimer(((TraceState) state).operationName, "validation"))
    );
  }

  @Override
  public InstrumentationContext<ExecutionResult> beginExecuteOperation(
    InstrumentationExecuteOperationParameters parameters,
    InstrumentationState state
  ) {
    return noOp();
  }

  @Override
  public ExecutionStrategyInstrumentationContext beginExecutionStrategy(
    InstrumentationExecutionStrategyParameters parameters,
    InstrumentationState state
  ) {
    return new ExecutionStrategyInstrumentationContext() {
      @Override
      public void onDispatched() {}

      @Override
      public void onCompleted(ExecutionResult result, Throwable t) {}
    };
  }

  @Override
  public InstrumentationContext<Object> beginFieldFetch(
    InstrumentationFieldFetchParameters parameters,
    InstrumentationState state
  ) {
    if (parameters.getField().getDirective("timingData") == null) {
      return noOp();
    }
    Timer.Sample sample = Timer.start(meterRegistry);
    return whenCompleted((res, err) -> {
      String parentType = GraphQLTypeUtil.simplePrint(
        parameters.getExecutionStepInfo().getParent().getUnwrappedNonNullType()
      );
      String fieldName = parameters.getExecutionStepInfo().getFieldDefinition().getName();
      sample.stop(buildFieldTimer(((TraceState) state).operationName, parentType, fieldName));
    });
  }

  private Timer buildQueryTimer(String operationName, String operation) {
    return Timer.builder(QUERY_TIME_METRIC_NAME)
      .description(TIMER_DESCRIPTION)
      .tag(OPERATION_NAME_TAG, operationName)
      .tag(OPERATION, operation)
      .tags(tags)
      .register(meterRegistry);
  }

  private Timer buildFieldTimer(String operationName, String parent, String field) {
    return Timer.builder(RESOLVER_TIME_METRIC_NAME)
      .description(TIMER_DESCRIPTION)
      .tag(OPERATION_NAME_TAG, operationName)
      .tag(PARENT, parent)
      .tag(FIELD, field)
      .tags(tags)
      .register(meterRegistry);
  }

  private record TraceState(String operationName) implements InstrumentationState {
    private TraceState(String operationName) {
      this.operationName = operationName == null ? "__UNKNOWN__" : operationName;
    }
  }
}
