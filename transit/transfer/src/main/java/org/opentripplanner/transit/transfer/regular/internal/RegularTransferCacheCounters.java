package org.opentripplanner.transit.transfer.regular.internal;

import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheMetrics;

/**
 * The counters of the caches of all snapshots, see {@link RegularTransferCacheMetrics}. The
 * lifecycle owns one instance, and each cache it creates counts into it.
 * <p>
 * Counting is a lock-free increment of a {@link LongAdder}, it does not allocate. The only
 * reference to a cache is the one of the latest frozen snapshot, used to read the size. It is
 * replaced each time a snapshot is frozen, so the counters never keep an older cache alive.
 * <p>
 * This class is thread-safe.
 */
final class RegularTransferCacheCounters implements RegularTransferCacheMetrics {

  private final LongAdder hits = new LongAdder();
  private final LongAdder misses = new LongAdder();
  private final LongAdder puts = new LongAdder();
  private final LongAdder evictions = new LongAdder();
  private final LongAdder loadFailures = new LongAdder();
  private final LongAdder loadNanos = new LongAdder();

  @Nullable
  private volatile RegularTransferServiceCache<?> latest;

  /**
   * Read the size from {@code cache}. The cache of a frozen snapshot that is never published has
   * the same values as the published one, so the size is correct either way.
   */
  void setLatest(RegularTransferServiceCache<?> cache) {
    this.latest = cache;
  }

  void hit() {
    hits.increment();
  }

  void miss() {
    misses.increment();
  }

  void created(long nanos) {
    puts.increment();
    loadNanos.add(nanos);
  }

  void creationFailed(long nanos) {
    loadFailures.increment();
    loadNanos.add(nanos);
  }

  void evicted() {
    evictions.increment();
  }

  @Override
  public long size() {
    var cache = latest;
    return cache == null ? 0 : cache.size();
  }

  @Override
  public long hitCount() {
    return hits.sum();
  }

  @Override
  public long missCount() {
    return misses.sum();
  }

  @Override
  public long putCount() {
    return puts.sum();
  }

  @Override
  public long evictionCount() {
    return evictions.sum();
  }

  @Override
  public long loadFailureCount() {
    return loadFailures.sum();
  }

  @Override
  public long totalLoadTimeNanos() {
    return loadNanos.sum();
  }
}
