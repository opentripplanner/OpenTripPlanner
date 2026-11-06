package org.opentripplanner.transit.transfer.regular.api.report;

/**
 * The counters of the cache of the Raptor transfers, for monitoring. They are cumulative and shared
 * by the caches of all snapshots, so they keep counting when a new snapshot is published. There is
 * one set of counters, nothing is counted per key, so the number of metrics is fixed.
 * <p>
 * A request is a <em>hit</em> if the Raptor transfers were already created when it asked for them,
 * and a <em>miss</em> if it created them or waited for another request to create them.
 * <p>
 * Implementations are thread-safe, and reading the counters never blocks a request.
 */
public interface RegularTransferCacheMetrics {
  /** The number of Raptor transfers in the cache now, including the ones of profiles. */
  long size();

  long hitCount();

  long missCount();

  /** The number of Raptor transfers created, from scratch or by a patch. */
  long putCount();

  /** The number of Raptor transfers evicted to make room for others. */
  long evictionCount();

  /** The number of creations that failed. */
  long loadFailureCount();

  /** The total time spent creating Raptor transfers, in nanoseconds, failures included. */
  long totalLoadTimeNanos();
}
