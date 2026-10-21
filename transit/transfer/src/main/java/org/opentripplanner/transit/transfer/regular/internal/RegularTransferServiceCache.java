package org.opentripplanner.transit.transfer.regular.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * Bounded LRU cache of the Raptor transfers derived from the paths of one snapshot, keyed on the
 * request-time user preferences. Each {@link DefaultRegularTransferRepositorySnapshot} owns one.
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
 * Each entry holds the profile its value is derived from, resolved once when the entry is
 * created. Normalizing the preferences, so that equivalent requests share an entry, is the
 * caller's responsibility, not this cache's.
 * <p>
 * This class is thread-safe.
 *
 * @param <V> the cached value type
 */
class RegularTransferServiceCache<V> {

  private static final int DEFAULT_MAX_SIZE = 32;

  private final int maxSize;
  private final ConcurrentHashMap<AbstractUserPreferences<?>, Entry<V>> entries =
    new ConcurrentHashMap<>();

  /** Orders the uses of the entries, to evict the least recently used one. */
  private final AtomicLong useCounter;

  RegularTransferServiceCache() {
    this(DEFAULT_MAX_SIZE);
  }

  RegularTransferServiceCache(int maxSize) {
    this(maxSize, 0);
  }

  private RegularTransferServiceCache(int maxSize, long lastUse) {
    this.maxSize = maxSize;
    this.useCounter = new AtomicLong(lastUse);
  }

  /**
   * Return the cached value for {@code userPreferences}. On a miss, create it with
   * {@code factory}; for a stale entry, patch the old value with {@code factory}.
   */
  V get(AbstractUserPreferences<?> userPreferences, Factory<V> factory) {
    var entry = entries.get(userPreferences);
    if (entry == null) {
      var created = Entry.<V>missing(factory.profileFor(userPreferences));
      entry = entries.putIfAbsent(userPreferences, created);
      if (entry == null) {
        entry = created;
        evictIfFull(userPreferences);
      }
    }
    entry.lastUse = useCounter.incrementAndGet();
    if (entry.claimed.compareAndSet(false, true)) {
      create(userPreferences, entry, factory);
    }
    return await(entry);
  }

  /**
   * The profile the value for {@code userPreferences} is derived from: the one of the cached
   * entry, or resolved with {@code factory} if there is none. Does not create an entry.
   */
  @Nullable
  AbstractUserPreferences<?> profileFor(
    AbstractUserPreferences<?> userPreferences,
    Factory<V> factory
  ) {
    var entry = entries.get(userPreferences);
    return entry != null ? entry.profile : factory.profileFor(userPreferences);
  }

  /**
   * Create the cache of the next snapshot. Each created value is kept, and marked stale if the
   * transfers from some stops changed under its profile. A stale value that was never patched
   * stays stale, with the changed stops added. A value that is still being created, or failed, is
   * dropped - the next snapshot creates it from scratch.
   *
   * @param changedFromStops the stops whose transfers changed, by profile
   */
  RegularTransferServiceCache<V> carryForward(
    Map<AbstractUserPreferences<?>, Set<Integer>> changedFromStops
  ) {
    var next = new RegularTransferServiceCache<V>(maxSize, useCounter.get());
    entries.forEach((userPreferences, entry) -> {
      V previous;
      Set<Integer> stale;
      if (entry.claimed.get()) {
        if (!entry.value.isDone() || entry.value.isCompletedExceptionally()) {
          return;
        }
        previous = entry.value.join();
        stale = Set.of();
      } else {
        previous = entry.previous;
        stale = entry.changedFromStops;
      }
      var changed = entry.profile == null ? null : changedFromStops.get(entry.profile);
      if (changed != null && !changed.isEmpty()) {
        var union = new HashSet<>(stale);
        union.addAll(changed);
        stale = union;
      }
      var carried = stale.isEmpty()
        ? Entry.created(entry.profile, previous)
        : Entry.stale(entry.profile, previous, stale);
      carried.lastUse = entry.lastUse;
      next.entries.put(userPreferences, carried);
    });
    return next;
  }

  private void create(AbstractUserPreferences<?> userPreferences, Entry<V> entry, Factory<V> f) {
    try {
      var value =
        entry.previous == null
          ? f.create(userPreferences, entry.profile)
          : f.patch(userPreferences, entry.profile, entry.previous, entry.changedFromStops);
      entry.value.complete(value);
    } catch (RuntimeException | Error e) {
      entries.remove(userPreferences, entry);
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
      entries.remove(eldest.get().getKey(), eldest.get().getValue());
    }
  }

  /** Creates the cached values. */
  interface Factory<V> {
    /**
     * The profile whose paths the value for {@code userPreferences} is derived from. {@code null}
     * if there are no paths.
     */
    @Nullable
    AbstractUserPreferences<?> profileFor(AbstractUserPreferences<?> userPreferences);

    /** Create the value for {@code userPreferences} from scratch. */
    V create(
      AbstractUserPreferences<?> userPreferences,
      @Nullable AbstractUserPreferences<?> profile
    );

    /**
     * Create the value for {@code userPreferences} from {@code previous}, the value of an earlier
     * snapshot, by updating the transfers from {@code changedFromStops}.
     */
    V patch(
      AbstractUserPreferences<?> userPreferences,
      @Nullable AbstractUserPreferences<?> profile,
      V previous,
      Set<Integer> changedFromStops
    );
  }

  /**
   * A cache entry. The value is created (or patched) by the request that claims the entry first;
   * all other requests wait for {@link #value}.
   */
  private static final class Entry<V> {

    @Nullable
    private final AbstractUserPreferences<?> profile;

    /** The value to patch, {@code null} to create the value from scratch. */
    @Nullable
    private final V previous;

    private final Set<Integer> changedFromStops;
    private final CompletableFuture<V> value = new CompletableFuture<>();
    private final AtomicBoolean claimed = new AtomicBoolean();
    private volatile long lastUse;

    private Entry(
      @Nullable AbstractUserPreferences<?> profile,
      @Nullable V previous,
      Set<Integer> changedFromStops
    ) {
      this.profile = profile;
      this.previous = previous;
      this.changedFromStops = changedFromStops;
    }

    static <V> Entry<V> missing(@Nullable AbstractUserPreferences<?> profile) {
      return new Entry<>(profile, null, Set.of());
    }

    static <V> Entry<V> stale(
      @Nullable AbstractUserPreferences<?> profile,
      V previous,
      Set<Integer> changedFromStops
    ) {
      return new Entry<>(profile, previous, Set.copyOf(changedFromStops));
    }

    static <V> Entry<V> created(@Nullable AbstractUserPreferences<?> profile, V value) {
      var entry = new Entry<V>(profile, null, Set.of());
      entry.claimed.set(true);
      entry.value.complete(value);
      return entry;
    }
  }
}
