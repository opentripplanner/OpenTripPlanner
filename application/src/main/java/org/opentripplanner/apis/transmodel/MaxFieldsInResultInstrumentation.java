package org.opentripplanner.apis.transmodel;

import static graphql.execution.instrumentation.SimpleInstrumentationContext.noOp;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import jakarta.validation.constraints.NotNull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A GraphQL instrumentation that aborts the execution if the number of fetched fields exceeds
 * a configurable limit.
 * The instrumentation also periodically checks the OTP request interruption status while the
 * query is being processed, giving the possibility to control the request runtime complexity
 * both in terms of result size and execution time.
 */
public class MaxFieldsInResultInstrumentation implements Instrumentation {

  private static final Logger LOG = LoggerFactory.getLogger(MaxFieldsInResultInstrumentation.class);

  /**
   * The maximum number of fields that can be present in the GraphQL result.
   */
  private final int maxFieldFetch;

  private final AtomicLong fieldFetchCounter = new AtomicLong();

  public MaxFieldsInResultInstrumentation(int maxFieldFetch) {
    this.maxFieldFetch = maxFieldFetch;
  }

  @Override
  public InstrumentationContext<Object> beginFieldFetch(
    InstrumentationFieldFetchParameters parameters,
    InstrumentationState state
  ) {
    long fetched = fieldFetchCounter.incrementAndGet();
    if (fetched % 10000 == 0) {
      LOG.debug("Fetched {} fields", fetched);
      if (fetched > maxFieldFetch) {
        throw new ResponseTooLargeException(
          "The number of fields in the GraphQL result exceeds the maximum allowed: " + maxFieldFetch
        );
      }
      OTPRequestTimeoutException.checkForTimeout();
    }
    return noOp();
  }

  @Override
  @NotNull
  public CompletableFuture<ExecutionResult> instrumentExecutionResult(
    ExecutionResult executionResult,
    InstrumentationExecutionParameters parameters,
    InstrumentationState state
  ) {
    LOG.debug("The GraphQL result contains {} fields", fieldFetchCounter.get());
    return Instrumentation.super.instrumentExecutionResult(executionResult, parameters, state);
  }
}
