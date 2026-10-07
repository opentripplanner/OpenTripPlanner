package org.opentripplanner.transit.transfer.regular.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
 * The cache is shared by all requests reading the same snapshot, without locking: the entries are
 * an immutable map, replaced atomically when a service is added. A miss builds the service outside
 * of any lock, so a read never waits for a build. Concurrent misses on the same key may each build
 * the service; the first one published is kept and returned to all of them. A failed build is not
 * cached, so the next request retries it.
 * <p>
 * Normalizing the preferences so equivalent requests share a cache entry is the caller's
 * responsibility, not this cache's.
 */
class RegularTransferServiceCache {

  private static final int DEFAULT_MAX_SIZE = 32;

  private final int maxSize;
  private final AtomicReference<Map<CacheKey, Entry>> entries = new AtomicReference<>(Map.of());

  /** Orders the uses of the entries, to evict the least recently used one. */
  private final AtomicLong useCounter = new AtomicLong();

  RegularTransferServiceCache() {
    this(DEFAULT_MAX_SIZE);
  }

  RegularTransferServiceCache(int maxSize) {
    this.maxSize = maxSize;
  }

  /**
   * Return the cached service for {@code (profileType, preferences)}, or create it with
   * {@code factory} and cache it.
   */
  RaptorRegularTransferService getOrCreate(
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences,
    Supplier<RaptorRegularTransferService> factory
  ) {
    var key = new CacheKey(profileType, preferences);
    var cached = entries.get().get(key);
    if (cached != null) {
      return use(cached);
    }
    var built = new Entry(factory.get(), useCounter.incrementAndGet());
    return use(entries.updateAndGet(current -> withEntry(current, key, built)).get(key));
  }

  private RaptorRegularTransferService use(Entry entry) {
    entry.lastUse = useCounter.incrementAndGet();
    return entry.service;
  }

  /**
   * Return a new map with the entry added, evicting the least recently used other entry if the map
   * is full. Return the current map unchanged if another request has already added the key.
   */
  private Map<CacheKey, Entry> withEntry(Map<CacheKey, Entry> current, CacheKey key, Entry entry) {
    if (current.containsKey(key)) {
      return current;
    }
    var updated = new HashMap<>(current);
    if (updated.size() >= maxSize) {
      // The new entry may already be the oldest if the build took long, so it is not a candidate
      updated
        .entrySet()
        .stream()
        .min((a, b) -> Long.compare(a.getValue().lastUse, b.getValue().lastUse))
        .ifPresent(eldest -> updated.remove(eldest.getKey()));
    }
    updated.put(key, entry);
    return Map.copyOf(updated);
  }

  private static final class Entry {

    private final RaptorRegularTransferService service;
    private volatile long lastUse;

    private Entry(RaptorRegularTransferService service, long lastUse) {
      this.service = service;
      this.lastUse = lastUse;
    }
  }

  private record CacheKey(
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences
  ) {}
}
