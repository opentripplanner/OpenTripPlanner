package org.opentripplanner.ext.reportapi.model;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

/**
 * The purpose of this class is to be a generic container for caching expensive computations.
 * <p>
 * THIS CLASS IS THREAD-SAFE.
 */
public class CachedValue<T> {

  private final Duration cacheInterval;
  private T value;
  private Instant timeout;

  public CachedValue(@Nonnull Duration cacheInterval) {
    this.value = null;
    this.cacheInterval = cacheInterval;
    this.timeout = calculateTimeout();
  }

  /**
   * If the cached value has not expired, then return it.
   * <p>
   * Otherwise, recompute and return it.
   */
  public T get(@Nonnull Supplier<T> supplier) {
    synchronized (this) {
      if (hasExpired()) {
        this.value = supplier.get();
        this.timeout = calculateTimeout();
      }
    }
    return value;
  }

  private Instant calculateTimeout() {
    return Instant.now().plus(cacheInterval);
  }

  private boolean hasExpired() {
    return value == null || timeout.isBefore(Instant.now());
  }
}
