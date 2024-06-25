package org.opentripplanner.apis.transmodel.support;

import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.ExecutionStrategyParameters;
import graphql.schema.DataFetchingEnvironment;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import org.opentripplanner.apis.transmodel.ResponseTooLargeException;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To abort fetching data when a request is unprocessable (either because the execution times
 * out or because the response is too large) we have to rethrow the exception.
 * This will prevent unresolved data-fetchers to be called. The exception is not handled
 * gracefully.
 */
public class AbortOnUnprocessableRequestExecutionStrategy
  extends AsyncExecutionStrategy
  implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(
    AbortOnUnprocessableRequestExecutionStrategy.class
  );
  public static final int LOG_STEPS = 25_000;
  private final ProgressTracker timeoutProgressTracker = ProgressTracker.track(
    "Unprocessable request. Abort GraphQL query",
    LOG_STEPS,
    -1
  );

  @Override
  protected <T> CompletableFuture<T> handleFetchingException(
    DataFetchingEnvironment environment,
    ExecutionStrategyParameters params,
    Throwable e
  ) {
    if (e instanceof OTPRequestTimeoutException || e instanceof ResponseTooLargeException) {
      logCancellationProgress();
      throw (RuntimeException) e;
    }
    return super.handleFetchingException(environment, params, e);
  }

  @SuppressWarnings("Convert2MethodRef")
  private void logCancellationProgress() {
    timeoutProgressTracker.startOrStep(m -> LOG.info(m));
  }

  @SuppressWarnings("Convert2MethodRef")
  @Override
  public void close() {
    timeoutProgressTracker.completeIfHasSteps(m -> LOG.info(m));
  }
}
