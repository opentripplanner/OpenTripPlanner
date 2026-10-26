package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.report.TransferCacheReportEntry;
import org.opentripplanner.transit.transfer.regular.api.report.UserPreferenceSource;

/**
 * The number of requests that used each key of the cache of the Raptor transfers, for the report
 * of the cache. The statistics outlive the snapshots, the keys of all snapshots are counted
 * together.
 * <p>
 * The number of keys is bounded: if the limit is reached, all keys with one hit are dropped. If that
 * does not make room, the keys with two hits or less are dropped, and so on until there is room. A
 * dropped key starts again with a count of one if it is used again, so the counts are approximate
 * if there are many different keys.
 * <p>
 * Counting is not on the lock path of the requests: a request does a lookup and increments a
 * counter. The statistics can be {@link #NONE}, which does nothing.
 * <p>
 * This class is thread-safe.
 */
abstract class RegularTransferCacheStatistics {

  /** The smallest number of keys tracked. */
  static final int MIN_CAPACITY = 100;

  /** The number of keys tracked is this many times the size of the cache, if it is above the min. */
  static final int CACHE_SIZE_FACTOR = 3;

  /** No statistics are collected. */
  static final RegularTransferCacheStatistics NONE = new RegularTransferCacheStatistics() {
    @Override
    void record(AbstractUserPreferences<?> key) {}

    @Override
    List<TransferCacheReportEntry> entries(
      Predicate<AbstractUserPreferences<?>> isCached,
      Function<AbstractUserPreferences<?>, KeyInfo> keyInfo
    ) {
      return List.of();
    }
  };

  /** The statistics of a cache with the given max size. */
  static RegularTransferCacheStatistics of(int cacheMaxSize) {
    return new Bounded(Math.max(MIN_CAPACITY, CACHE_SIZE_FACTOR * cacheMaxSize));
  }

  /** The statistics with a given max number of keys, for tests. */
  static RegularTransferCacheStatistics withCapacity(int capacity) {
    return new Bounded(capacity);
  }

  /**
   * What is known about a key, resolved when the report is built.
   *
   * @param id               the id of the profile or the warmup entry, {@code null} if on demand
   * @param closestProfileId the id of the closest profile, {@code null} if there is none
   */
  record KeyInfo(
    UserPreferenceSource source,
    @Nullable String id,
    @Nullable String closestProfileId
  ) {}

  /** Count a request that used {@code key}. */
  abstract void record(AbstractUserPreferences<?> key);

  /**
   * The tracked keys, the most used first.
   *
   * @param isCached whether the Raptor transfers of a key are in the cache now
   * @param keyInfo  resolves what is known about a key, it is only called when the report is built
   */
  abstract List<TransferCacheReportEntry> entries(
    Predicate<AbstractUserPreferences<?>> isCached,
    Function<AbstractUserPreferences<?>, KeyInfo> keyInfo
  );

  private static final class Bounded extends RegularTransferCacheStatistics {

    private final int capacity;
    private final ConcurrentHashMap<AbstractUserPreferences<?>, Counter> counters =
      new ConcurrentHashMap<>();

    private Bounded(int capacity) {
      this.capacity = capacity;
    }

    @Override
    void record(AbstractUserPreferences<?> key) {
      var counter = counters.get(key);
      if (counter == null) {
        if (counters.size() >= capacity) {
          makeRoom();
        }
        counter = counters.computeIfAbsent(key, k -> new Counter());
      }
      counter.hits.increment();
    }

    @Override
    List<TransferCacheReportEntry> entries(
      Predicate<AbstractUserPreferences<?>> isCached,
      Function<AbstractUserPreferences<?>, KeyInfo> keyInfo
    ) {
      return counters
        .entrySet()
        .stream()
        .map(e -> {
          var info = keyInfo.apply(e.getKey());
          return new TransferCacheReportEntry(
            e.getKey().type(),
            info.source(),
            info.id(),
            info.closestProfileId(),
            e.getKey().toString(),
            e.getValue().hits.sum(),
            isCached.test(e.getKey())
          );
        })
        .sorted(Comparator.comparingLong(TransferCacheReportEntry::hits).reversed())
        .toList();
    }

    /**
     * Drop the keys with the fewest hits: all keys with one hit, and if that does not make room,
     * all keys with two hits or less, and so on. A hit counted while the keys are dropped may be
     * lost, the counts are approximate.
     */
    private void makeRoom() {
      for (long limit = 1; counters.size() >= capacity; ++limit) {
        final long hitsLimit = limit;
        counters.values().removeIf(counter -> counter.hits.sum() <= hitsLimit);
      }
    }
  }

  private static final class Counter {

    private final LongAdder hits = new LongAdder();
  }
}
