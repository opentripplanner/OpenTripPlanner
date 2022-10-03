package org.opentripplanner.common.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

/**
 * The purpose of this class is to be a generic container for caching expensive computations.
 * <p>
 * THIS CLASS IS THREAD-SAFE.
 */
public class CachedValue<T> {

  private final Supplier<T> supplier;
  private final Duration cacheInterval;
  private T value;
  private Instant timeout;

  public CachedValue(@Nonnull Supplier<T> supplier, @Nonnull Duration cacheInterval) {
    this.supplier = Objects.requireNonNull(supplier);
    this.value = null;
    this.cacheInterval = cacheInterval;
    this.timeout = calculateTimeout();
  }

  /**
   * If the cached value has not expired, then return it.
   * <p>
   * Otherwise, recompute and return it.
   */
  public T get() {
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
