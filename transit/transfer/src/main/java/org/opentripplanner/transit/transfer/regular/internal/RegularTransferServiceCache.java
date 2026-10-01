package org.opentripplanner.transit.transfer.regular.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.opentripplanner.transit.transfer.regular.RaptorRegularTransferService;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Bounded LRU cache of {@link RaptorRegularTransferService}s, keyed on
 * {@code (profileType, preferences)}. Each {@link DefaultRegularTransferRepositorySnapshot} owns
 * one, and a new snapshot starts with an empty cache - a cached service is built from the paths of
 * the snapshot it belongs to.
 * <p>
 * The cache is shared by all requests reading the same snapshot, so access is synchronized.
 * Coarse-grained lock: a cache miss builds the whole service inside the lock, which is CPU-bound,
 * not I/O - acceptable for the expected handful of distinct combinations, revisit if contention
 * shows up under load.
 * <p>
 * Normalizing the preferences so equivalent requests share a cache entry is the caller's
 * responsibility, not this cache's.
 */
class RegularTransferServiceCache {

  private static final int DEFAULT_MAX_SIZE = 32;

  private final Map<CacheKey, RaptorRegularTransferService> cache;

  RegularTransferServiceCache() {
    this(DEFAULT_MAX_SIZE);
  }

  RegularTransferServiceCache(int maxSize) {
    this.cache = new LinkedHashMap<>(16, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(
        Map.Entry<CacheKey, RaptorRegularTransferService> eldest
      ) {
        return size() > maxSize;
      }
    };
  }

  /**
   * Return the cached service for {@code (profileType, preferences)}, or create it with
   * {@code factory} and cache it.
   */
  synchronized RaptorRegularTransferService getOrCreate(
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences,
    Supplier<RaptorRegularTransferService> factory
  ) {
    return cache.computeIfAbsent(new CacheKey(profileType, preferences), _ -> factory.get());
  }

  private record CacheKey(
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences
  ) {}
}
