package org.opentripplanner.apis.transmodel.support;

import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.DataFetchingEnvironment;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To abort fetching data when a timeout occurs we have to rethrow the time-out-exception.
 * This will prevent unresolved data-fetchers to be called. The exception is not handled
 * gracefully.
 */
public class AbortOnTimeoutExecutionStrategy extends AsyncExecutionStrategy implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(AbortOnTimeoutExecutionStrategy.class);
  public static final int LOG_STEPS = 25_000;
  private final ProgressTracker timeoutProgressTracker = ProgressTracker.track(
    "TIMEOUT! Abort GraphQL query",
    LOG_STEPS,
    -1
  );

  @Override
  protected <T> CompletableFuture<T> handleFetchingException(
    DataFetchingEnvironment environment,
    Throwable e
  ) {
    if (e instanceof OTPRequestTimeoutException te) {
      logTimeoutProgress();
      throw te;
    }
    return super.handleFetchingException(environment, e);
  }

  @SuppressWarnings("Convert2MethodRef")
  private void logTimeoutProgress() {
    timeoutProgressTracker.startOrStep(m -> LOG.info(m));
  }

  @SuppressWarnings("Convert2MethodRef")
  @Override
  public void close() {
    timeoutProgressTracker.completeIfHasSteps(m -> LOG.info(m));
  }
}
