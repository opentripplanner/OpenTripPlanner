package org.opentripplanner.framework.retry;

import java.time.Duration;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retry an operation with a configurable number of attempts.
 */
public class OtpRetry {

  private static final Logger LOG = LoggerFactory.getLogger(OtpRetry.class);

  private final String name;
  private final int maxAttempts;
  private final Duration initialRetryInterval;
  private final int backoffMultiplier;
  private final Runnable onRetry;
  private final Predicate<Exception> retryableException;

  OtpRetry(
    String name,
    int maxAttempts,
    Duration initialRetryInterval,
    int backoffMultiplier,
    Predicate<Exception> retryableException,
    Runnable onRetry
  ) {
    this.name = name;
    this.maxAttempts = maxAttempts;
    this.initialRetryInterval = initialRetryInterval;
    this.backoffMultiplier = backoffMultiplier;
    this.retryableException = retryableException;
    this.onRetry = onRetry;
  }

  public void execute(Runnable retryable) throws InterruptedException {
    int attempts = 0;
    long sleepTime = initialRetryInterval.toMillis();
    while (true) {
      try {
        retryable.run();
        return;
      } catch (Exception e) {
        if (!retryableException.test(e)) {
          throw new OtpRetryException("Operation failed with non-retryable exception", e);
        }
        attempts++;
        if (attempts > maxAttempts) {
          throw new OtpRetryException("Operation failed after " + attempts + " attempts", e);
        }
        LOG.info(
          "Operation {} failed with retryable exception: {}. Retrying {}/{} in {} millis",
          name,
          e.getMessage(),
          attempts,
          maxAttempts,
          sleepTime
        );
        onRetry.run();
        Thread.sleep(sleepTime);
        sleepTime = sleepTime * backoffMultiplier;
      }
    }
  }
}
