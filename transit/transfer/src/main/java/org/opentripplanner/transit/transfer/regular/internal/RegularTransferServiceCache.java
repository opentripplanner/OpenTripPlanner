package org.opentripplanner.transit.transfer.regular.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheMetrics;

/**
 * Cache of the Raptor transfers derived from the paths of one snapshot, keyed on the request-time
 * user preferences. Each {@link DefaultRegularTransferRepositorySnapshot} owns one.
 * <p>
 * <b>Retained keys.</b> The keys given on construction - the preferences of the profiles and of
 * the warmup entries - are retained: they are never evicted and do not count against the max size.
 * The set of retained keys is immutable, and is passed on to the cache of the next snapshot. All
 * other keys are cached in a bounded LRU of at most {@code maxSize} values.
 * <p>
 * <b>Single-flight.</b> The first request missing a key creates the value; concurrent requests
 * for the same key wait for it instead of creating it again. Requests for other keys never wait.
 * A failed creation is not cached: all waiting requests get the exception, and the next request
 * tries again.
 * <p>
 * <b>Carried forward.</b> When a new snapshot is published, its cache is created from the cache of
 * the previous snapshot with {@link #carryForward}. A value is kept if the paths it was derived
 * from did not change; otherwise it is marked stale, with the stops whose transfers changed. The
 * first request for a stale key patches the old value instead of creating it from scratch, see
 * {@link Factory#patch}. A stale entry keeps the old value alive until it is patched or evicted.
 * <p>
 * Normalizing the preferences, so that equivalent requests share an entry, is the caller's
 * responsibility, not this cache's.
 * <p>
 * This class is thread-safe.
 *
 * @param <V> the cached value type
 */
class RegularTransferServiceCache<V> {

  static final int DEFAULT_MAX_SIZE = 32;

  private final int maxSize;

  /**
   * The slots of the retained keys. The map is immutable, and a slot always holds an entry. The
   * entry of a slot is only replaced when its creation failed.
   */
  private final Map<AbstractUserPreferences<?>, AtomicReference<Entry<V>>> retained;

  /** The entries of all other keys, bounded by {@link #maxSize}. */
  private final ConcurrentHashMap<AbstractUserPreferences<?>, Entry<V>> entries =
    new ConcurrentHashMap<>();

  /** Orders the uses of the entries, to evict the least recently used one. */
  private final AtomicLong useCounter;

  /** Shared by the caches of all snapshots. */
  private final RegularTransferCacheCounters counters;

  RegularTransferServiceCache() {
    this(DEFAULT_MAX_SIZE);
  }

  RegularTransferServiceCache(int maxSize) {
    this(maxSize, Set.of());
  }

  /**
   * @param maxSize      the max number of values in the cache, not counting the retained ones
   * @param retainedKeys the keys that are never evicted
   */
  RegularTransferServiceCache(int maxSize, Set<AbstractUserPreferences<?>> retainedKeys) {
    this(maxSize, retainedKeys, new RegularTransferCacheCounters());
  }

  /**
   * @param counters the counters this cache counts into, shared by the caches of all snapshots
   */
  RegularTransferServiceCache(
    int maxSize,
    Set<AbstractUserPreferences<?>> retainedKeys,
    RegularTransferCacheCounters counters
  ) {
    this(maxSize, retainedKeys, counters, 0);
  }

  private RegularTransferServiceCache(
    int maxSize,
    Set<AbstractUserPreferences<?>> retainedKeys,
    RegularTransferCacheCounters counters,
    long lastUse
  ) {
    this.maxSize = maxSize;
    this.counters = counters;
    var slots = new HashMap<AbstractUserPreferences<?>, AtomicReference<Entry<V>>>();
    retainedKeys.forEach(key -> slots.put(key, new AtomicReference<>(Entry.missing())));
    this.retained = Map.copyOf(slots);
    this.useCounter = new AtomicLong(lastUse);
  }

  /**
   * Return the cached value for {@code userPreferences}. On a miss, create it with
   * {@code factory}; for a stale entry, patch the old value with {@code factory}.
   */
  V get(AbstractUserPreferences<?> userPreferences, Factory<V> factory) {
    var slot = retained.get(userPreferences);
    if (slot != null) {
      var entry = slot.get();
      count(entry);
      claimAndCreate(userPreferences, entry, factory, () ->
        slot.compareAndSet(entry, Entry.missing())
      );
      return await(entry);
    }
    var entry = entries.get(userPreferences);
    if (entry == null) {
      var created = Entry.<V>missing();
      entry = entries.putIfAbsent(userPreferences, created);
      if (entry == null) {
        entry = created;
        evictIfFull(userPreferences);
      }
    }
    entry.lastUse = useCounter.incrementAndGet();
    count(entry);
    var claimed = entry;
    claimAndCreate(userPreferences, claimed, factory, () ->
      entries.remove(userPreferences, claimed)
    );
    return await(entry);
  }

  /** Whether the value of {@code userPreferences} has been created, and is in the cache. */
  boolean contains(AbstractUserPreferences<?> userPreferences) {
    var slot = retained.get(userPreferences);
    var entry = slot != null ? slot.get() : entries.get(userPreferences);
    return entry != null && entry.isCreated();
  }

  /**
   * The number of values in the cache now, including the retained ones. Values still being
   * created, and failed ones, are not counted. The size is not exact if the cache is used at the
   * same time.
   */
  int size() {
    long size = entries.values().stream().filter(Entry::isCreated).count();
    size += retained
      .values()
      .stream()
      .filter(slot -> slot.get().isCreated())
      .count();
    return (int) size;
  }

  /** The counters of this cache, shared by the caches of all snapshots. */
  RegularTransferCacheMetrics metrics() {
    return counters;
  }

  /** The max number of keys in the cache, not counting the retained keys. */
  int maxSize() {
    return maxSize;
  }

  /**
   * Create the cache of the next snapshot. Each created value is kept, and marked stale if the
   * transfers from some stops it is derived from changed. A stale value that was never patched
   * stays stale, with the changed stops added. A value that is still being created, or failed, is
   * dropped - the next snapshot creates it from scratch. The retained keys are the same.
   *
   * @param changedFromStops the stops whose transfers changed, for the user preferences of an
   *                         entry; empty if none changed
   */
  RegularTransferServiceCache<V> carryForward(
    Function<AbstractUserPreferences<?>, Set<Integer>> changedFromStops
  ) {
    var next = new RegularTransferServiceCache<V>(
      maxSize,
      retained.keySet(),
      counters,
      useCounter.get()
    );
    retained.forEach((userPreferences, slot) -> {
      var carried = carry(slot.get(), changedFromStops.apply(userPreferences));
      if (carried != null) {
        next.retained.get(userPreferences).set(carried);
      }
    });
    entries.forEach((userPreferences, entry) -> {
      var carried = carry(entry, changedFromStops.apply(userPreferences));
      if (carried != null) {
        next.entries.put(userPreferences, carried);
      }
    });
    return next;
  }

  /** The entry for the next snapshot, {@code null} if there is nothing to carry forward. */
  @Nullable
  private static <V> Entry<V> carry(Entry<V> entry, Set<Integer> changed) {
    V previous;
    Set<Integer> stale;
    if (entry.claimed.get()) {
      if (!entry.value.isDone() || entry.value.isCompletedExceptionally()) {
        return null;
      }
      previous = entry.value.join();
      stale = Set.of();
    } else {
      previous = entry.previous;
      stale = entry.changedFromStops;
    }
    if (previous == null) {
      // Not created yet, nothing to carry forward
      return null;
    }
    if (!changed.isEmpty()) {
      var union = new HashSet<>(stale);
      union.addAll(changed);
      stale = union;
    }
    var carried = stale.isEmpty() ? Entry.created(previous) : Entry.stale(previous, stale);
    carried.lastUse = entry.lastUse;
    return carried;
  }

  /** A request is a hit if the value is created, and a miss if it has to create or wait for it. */
  private void count(Entry<V> entry) {
    if (entry.value.isDone()) {
      counters.hit();
    } else {
      counters.miss();
    }
  }

  private void claimAndCreate(
    AbstractUserPreferences<?> userPreferences,
    Entry<V> entry,
    Factory<V> factory,
    Runnable onFailure
  ) {
    if (entry.claimed.compareAndSet(false, true)) {
      create(userPreferences, entry, factory, onFailure);
    }
  }

  private void create(
    AbstractUserPreferences<?> userPreferences,
    Entry<V> entry,
    Factory<V> f,
    Runnable onFailure
  ) {
    long start = System.nanoTime();
    try {
      var value =
        entry.previous == null
          ? f.create(userPreferences)
          : f.patch(userPreferences, entry.previous, entry.changedFromStops);
      counters.created(System.nanoTime() - start);
      entry.value.complete(value);
    } catch (RuntimeException | Error e) {
      counters.creationFailed(System.nanoTime() - start);
      onFailure.run();
      entry.value.completeExceptionally(e);
      throw e;
    }
  }

  private static <V> V await(Entry<V> entry) {
    try {
      return entry.value.join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof RuntimeException re) {
        throw re;
      }
      if (e.getCause() instanceof Error error) {
        throw error;
      }
      throw e;
    }
  }

  /** Evict the least recently used entries other than {@code added}, if the cache is full. */
  private void evictIfFull(AbstractUserPreferences<?> added) {
    while (entries.size() > maxSize) {
      var eldest = entries
        .entrySet()
        .stream()
        .filter(e -> !e.getKey().equals(added))
        .min((a, b) -> Long.compare(a.getValue().lastUse, b.getValue().lastUse));
      if (eldest.isEmpty()) {
        return;
      }
      if (entries.remove(eldest.get().getKey(), eldest.get().getValue())) {
        counters.evicted();
      }
    }
  }

  /** Creates the cached values. */
  interface Factory<V> {
    /** Create the value for {@code userPreferences} from scratch. */
    V create(AbstractUserPreferences<?> userPreferences);

    /**
     * Create the value for {@code userPreferences} from {@code previous}, the value of an earlier
     * snapshot, by updating the transfers from {@code changedFromStops}.
     */
    V patch(AbstractUserPreferences<?> userPreferences, V previous, Set<Integer> changedFromStops);
  }

  /**
   * A cache entry. The value is created (or patched) by the request that claims the entry first;
   * all other requests wait for {@link #value}.
   */
  private static final class Entry<V> {

    /** The value to patch, {@code null} to create the value from scratch. */
    @Nullable
    private final V previous;

    private final Set<Integer> changedFromStops;
    private final CompletableFuture<V> value = new CompletableFuture<>();
    private final AtomicBoolean claimed = new AtomicBoolean();
    private volatile long lastUse;

    boolean isCreated() {
      return value.isDone() && !value.isCompletedExceptionally();
    }

    private Entry(@Nullable V previous, Set<Integer> changedFromStops) {
      this.previous = previous;
      this.changedFromStops = changedFromStops;
    }

    static <V> Entry<V> missing() {
      return new Entry<>(null, Set.of());
    }

    static <V> Entry<V> stale(V previous, Set<Integer> changedFromStops) {
      return new Entry<>(previous, Set.copyOf(changedFromStops));
    }

    static <V> Entry<V> created(V value) {
      var entry = new Entry<V>(null, Set.of());
      entry.claimed.set(true);
      entry.value.complete(value);
      return entry;
    }
  }
}
