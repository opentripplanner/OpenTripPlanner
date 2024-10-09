package org.opentripplanner.framework.retry;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

public class OtpRetryBuilder {

  public static final int DEFAULT_MAX_ATTEMPTS = 3;
  public static final Duration DEFAULT_INITIAL_RETRYABLE_INTERVAL = Duration.of(
    1,
    ChronoUnit.SECONDS
  );
  /**
   * Retry all exceptions by default.
   */
  public static final Predicate<Exception> DEFAULT_RETRYABLE_EXCEPTION = e -> true;
  public static final Runnable DEFAULT_ON_RETRY = () -> {};
  private String name;
  private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
  private Duration initialRetryInterval = DEFAULT_INITIAL_RETRYABLE_INTERVAL;
  private int backoffMultiplier;
  private Predicate<Exception> retryableException = DEFAULT_RETRYABLE_EXCEPTION;
  private Runnable onRetry = DEFAULT_ON_RETRY;

  /**
   * Name used in log messages to identify the retried operation.
   */
  public OtpRetryBuilder withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Maximum number of additional attempts after the initial failure.
   * With maxAttempts=0 no retry is performed after the initial failure.
   */
  public OtpRetryBuilder withMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
    return this;
  }

  /**
   * Initial delay before the first retry.
   */
  public OtpRetryBuilder withInitialRetryInterval(Duration initialRetryInterval) {
    this.initialRetryInterval = initialRetryInterval;
    return this;
  }

  /**
   * Backoff multiplier applied to the initial delay.
   */
  public OtpRetryBuilder withBackoffMultiplier(int backoffMultiplier) {
    this.backoffMultiplier = backoffMultiplier;
    return this;
  }

  /**
   * Predicate identifying the exceptions that should be retried.
   * Other exceptions are re-thrown.
   */
  public OtpRetryBuilder withRetryableException(Predicate<Exception> retryableException) {
    this.retryableException = retryableException;
    return this;
  }

  /**
   * Callback invoked before executing each retry.
   */
  public OtpRetryBuilder withOnRetry(Runnable onRetry) {
    this.onRetry = onRetry;
    return this;
  }

  public OtpRetry build() {
    return new OtpRetry(
      name,
      maxAttempts,
      initialRetryInterval,
      backoffMultiplier,
      retryableException,
      onRetry
    );
  }
}
