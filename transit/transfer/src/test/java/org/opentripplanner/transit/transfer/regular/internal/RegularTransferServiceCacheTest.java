package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

class RegularTransferServiceCacheTest {

  private static final WalkPreferences PROFILE_1 = withReluctance(1.0);
  private static final WalkPreferences PROFILE_2 = withReluctance(4.0);

  private static final WalkPreferences PREFS_1 = withReluctance(1.0);
  private static final WalkPreferences PREFS_2 = withReluctance(2.0);
  private static final WalkPreferences PREFS_3 = withReluctance(3.0);
  private static final WalkPreferences PREFS_4 = withReluctance(4.0);

  private static final Function<AbstractUserPreferences<?>, Set<Integer>> NOTHING_CHANGED = p ->
    Set.of();

  @Test
  void createsOnceAndReturnsTheCachedValue() {
    var cache = new RegularTransferServiceCache<String>();
    var factory = new TestFactory();

    var first = cache.get(PREFS_1, factory);
    var second = cache.get(withReluctance(1.0), factory);

    assertThat(second).isSameInstanceAs(first);
    assertThat(factory.created).containsExactly("create " + PREFS_1);
  }

  @Test
  void differentPreferencesAreCachedSeparately() {
    var cache = new RegularTransferServiceCache<String>();
    var factory = new TestFactory();

    assertThat(cache.get(PREFS_1, factory)).isNotEqualTo(cache.get(PREFS_2, factory));
  }

  @Test
  void evictsTheLeastRecentlyUsedValue() {
    var cache = new RegularTransferServiceCache<String>(2);
    var factory = new TestFactory();
    cache.get(PREFS_1, factory);
    cache.get(PREFS_2, factory);

    // Use 1, so 2 becomes the least recently used and is evicted when 3 is added
    cache.get(PREFS_1, factory);
    cache.get(PREFS_3, factory);
    factory.created.clear();

    cache.get(PREFS_1, factory);
    cache.get(PREFS_2, factory);
    assertThat(factory.created).containsExactly("create " + PREFS_2);
  }

  @Test
  void aHitDoesNotWaitForTheCreationOfAnotherKey() throws Exception {
    var cache = new RegularTransferServiceCache<String>();
    var cached = cache.get(PREFS_2, new TestFactory());
    var creationStarted = new CountDownLatch(1);
    var releaseCreation = new CountDownLatch(1);
    var slow = new TestFactory(() -> {
      creationStarted.countDown();
      awaitOrFail(releaseCreation);
    });

    var executor = Executors.newSingleThreadExecutor();
    try {
      var slowCreation = executor.submit(() -> cache.get(PREFS_1, slow));
      awaitOrFail(creationStarted);

      // PREFS_1 is still being created, the cached PREFS_2 must be returned anyway
      var hit = assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
        cache.get(PREFS_2, new TestFactory())
      );
      assertThat(hit).isSameInstanceAs(cached);

      releaseCreation.countDown();
      slowCreation.get(5, TimeUnit.SECONDS);
    } finally {
      releaseCreation.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentMissesOnTheSameKeyCreateTheValueOnce() throws Exception {
    var cache = new RegularTransferServiceCache<String>();
    var creationStarted = new CountDownLatch(1);
    var releaseCreation = new CountDownLatch(1);
    var factory = new TestFactory(() -> {
      creationStarted.countDown();
      awaitOrFail(releaseCreation);
    });

    var executor = Executors.newFixedThreadPool(2);
    try {
      var first = executor.submit(() -> cache.get(PREFS_1, factory));
      awaitOrFail(creationStarted);
      var second = executor.submit(() -> cache.get(PREFS_1, factory));

      releaseCreation.countDown();
      assertThat(second.get(5, TimeUnit.SECONDS)).isSameInstanceAs(first.get(5, TimeUnit.SECONDS));
      assertThat(factory.created).hasSize(1);
    } finally {
      releaseCreation.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void aFailedCreationIsRetried() {
    var cache = new RegularTransferServiceCache<String>();
    var failing = new TestFactory(() -> {
      throw new IllegalStateException("Expected failure");
    });

    var ex = assertThrows(IllegalStateException.class, () -> cache.get(PREFS_1, failing));
    assertThat(ex).hasMessageThat().isEqualTo("Expected failure");

    var factory = new TestFactory();
    assertThat(cache.get(PREFS_1, factory)).isEqualTo("create " + PREFS_1);
  }

  @Test
  void carryForwardKeepsTheValuesOfUnchangedProfiles() {
    var cache = new RegularTransferServiceCache<String>();
    var factory = new TestFactory();
    var value = cache.get(PREFS_1, factory);

    var next = cache.carryForward(changed(PROFILE_2, 7));

    assertThat(next.get(PREFS_1, factory)).isSameInstanceAs(value);
    assertThat(factory.created).hasSize(1);
  }

  @Test
  void carryForwardPatchesTheValuesOfChangedProfilesOnFirstUse() {
    var cache = new RegularTransferServiceCache<String>();
    var factory = new TestFactory();
    var value1 = cache.get(PREFS_1, factory);
    var value4 = cache.get(PREFS_4, factory);

    var next = cache.carryForward(changed(PROFILE_1, 7));
    factory.created.clear();

    assertThat(next.get(PREFS_4, factory)).isSameInstanceAs(value4);
    assertThat(next.get(PREFS_1, factory)).isEqualTo("patch " + PREFS_1 + " " + Set.of(7));
    assertThat(factory.created).containsExactly("patch " + PREFS_1 + " " + Set.of(7));
    // The cache of the previous snapshot is not changed
    assertThat(cache.get(PREFS_1, factory)).isSameInstanceAs(value1);
  }

  @Test
  void aValueStaleForSeveralSnapshotsIsPatchedOnceForAllChanges() {
    var cache = new RegularTransferServiceCache<String>();
    var factory = new TestFactory();
    cache.get(PREFS_1, factory);

    var next = cache
      .carryForward(changed(PROFILE_1, 7))
      .carryForward(NOTHING_CHANGED)
      .carryForward(changed(PROFILE_1, 8));
    factory.created.clear();

    next.get(PREFS_1, factory);
    assertThat(factory.patched).containsExactly(Set.of(7, 8));
  }

  @Test
  void carryForwardDropsValuesThatFailedOrAreStillBeingCreated() throws Exception {
    var cache = new RegularTransferServiceCache<String>();
    var creationStarted = new CountDownLatch(1);
    var releaseCreation = new CountDownLatch(1);
    var slow = new TestFactory(() -> {
      creationStarted.countDown();
      awaitOrFail(releaseCreation);
    });

    var executor = Executors.newSingleThreadExecutor();
    try {
      var slowCreation = executor.submit(() -> cache.get(PREFS_1, slow));
      awaitOrFail(creationStarted);

      var next = cache.carryForward(NOTHING_CHANGED);
      releaseCreation.countDown();
      slowCreation.get(5, TimeUnit.SECONDS);

      var factory = new TestFactory();
      next.get(PREFS_1, factory);
      assertThat(factory.created).containsExactly("create " + PREFS_1);
    } finally {
      releaseCreation.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void retainedKeysAreNeverEvictedAndNotCountedInTheMaxSize() {
    var cache = new RegularTransferServiceCache<String>(1, Set.of(PREFS_1, PREFS_4));
    var factory = new TestFactory();
    cache.get(PREFS_1, factory);
    cache.get(PREFS_4, factory);

    // Fill and churn the bounded part of the cache
    cache.get(PREFS_2, factory);
    cache.get(PREFS_3, factory);
    factory.created.clear();

    cache.get(PREFS_1, factory);
    cache.get(PREFS_4, factory);
    assertThat(factory.created).isEmpty();
    assertThat(cache.contains(PREFS_1)).isTrue();
    assertThat(cache.contains(PREFS_2)).isFalse();
    assertThat(cache.contains(PREFS_3)).isTrue();
  }

  @Test
  void aFailedCreationOfARetainedKeyIsRetried() {
    var cache = new RegularTransferServiceCache<String>(1, Set.of(PREFS_1));
    var failing = new TestFactory(() -> {
      throw new IllegalStateException("Expected failure");
    });

    assertThrows(IllegalStateException.class, () -> cache.get(PREFS_1, failing));

    assertThat(cache.contains(PREFS_1)).isFalse();
    assertThat(cache.get(PREFS_1, new TestFactory())).isEqualTo("create " + PREFS_1);
  }

  @Test
  void carryForwardKeepsTheRetainedKeys() {
    var cache = new RegularTransferServiceCache<String>(1, Set.of(PREFS_1, PREFS_4));
    var factory = new TestFactory();
    var value1 = cache.get(PREFS_1, factory);
    // PREFS_4 is never used, so there is no value to carry forward

    var next = cache.carryForward(NOTHING_CHANGED);
    next.get(PREFS_2, factory);
    next.get(PREFS_3, factory);
    factory.created.clear();

    assertThat(next.get(PREFS_1, factory)).isSameInstanceAs(value1);
    assertThat(next.get(PREFS_4, factory)).isEqualTo("create " + PREFS_4);
    assertThat(factory.created).containsExactly("create " + PREFS_4);
  }

  @Test
  void carryForwardPatchesTheValuesOfChangedRetainedKeys() {
    var cache = new RegularTransferServiceCache<String>(1, Set.of(PREFS_1));
    var factory = new TestFactory();
    cache.get(PREFS_1, factory);

    var next = cache.carryForward(changed(PROFILE_1, 7));
    factory.created.clear();

    assertThat(next.get(PREFS_1, factory)).isEqualTo("patch " + PREFS_1 + " " + Set.of(7));
  }

  @Test
  void countsHitsMissesPutsAndEvictions() {
    var cache = new RegularTransferServiceCache<String>(1);
    var metrics = cache.metrics();
    var factory = new TestFactory();

    cache.get(PREFS_1, factory);
    cache.get(PREFS_1, factory);
    // Evicts PREFS_1, the cache holds one value
    cache.get(PREFS_2, factory);

    assertThat(metrics.missCount()).isEqualTo(2);
    assertThat(metrics.hitCount()).isEqualTo(1);
    assertThat(metrics.putCount()).isEqualTo(2);
    assertThat(metrics.evictionCount()).isEqualTo(1);
    assertThat(metrics.loadFailureCount()).isEqualTo(0);
    // No snapshot has published its cache yet
    assertThat(metrics.size()).isEqualTo(0);
  }

  @Test
  void countsFailedCreationsAndDoesNotCountThemAsPuts() {
    var cache = new RegularTransferServiceCache<String>();
    var failing = new TestFactory(() -> {
      throw new IllegalStateException("Expected failure");
    });

    assertThrows(IllegalStateException.class, () -> cache.get(PREFS_1, failing));

    assertThat(cache.metrics().loadFailureCount()).isEqualTo(1);
    assertThat(cache.metrics().putCount()).isEqualTo(0);
    assertThat(cache.size()).isEqualTo(0);
  }

  @Test
  void theSizeCountsRetainedAndBoundedValuesButNotValuesStillBeingCreated() {
    var cache = new RegularTransferServiceCache<String>(2, Set.of(PREFS_1));
    var factory = new TestFactory();
    assertThat(cache.size()).isEqualTo(0);

    cache.get(PREFS_1, factory);
    cache.get(PREFS_2, factory);
    cache.get(PREFS_3, factory);
    cache.get(PREFS_4, factory);

    assertThat(cache.size()).isEqualTo(3);
  }

  @Test
  void theCountersAreSharedWithTheCacheOfTheNextSnapshot() {
    var counters = new RegularTransferCacheCounters();
    var cache = new RegularTransferServiceCache<String>(2, Set.of(), counters);
    var factory = new TestFactory();
    cache.get(PREFS_1, factory);

    var next = cache.carryForward(NOTHING_CHANGED);
    next.get(PREFS_1, factory);

    assertThat(next.metrics()).isSameInstanceAs(cache.metrics());
    assertThat(counters.missCount()).isEqualTo(1);
    assertThat(counters.hitCount()).isEqualTo(1);
    counters.setLatest(next);
    assertThat(counters.size()).isEqualTo(1);
  }

  @Test
  void everyRequestIsCountedOnceWhenManyThreadsUseTheCache() throws Exception {
    int threads = 8;
    int requestsPerThread = 2_000;
    var keys = List.of(PREFS_1, PREFS_2, PREFS_3, PREFS_4);
    var cache = new RegularTransferServiceCache<String>(2, Set.of(PREFS_1));
    var factory = new TestFactory();
    var executor = Executors.newFixedThreadPool(threads);
    try {
      var futures = new ArrayList<Future<?>>();
      for (int t = 0; t < threads; t++) {
        int offset = t;
        futures.add(
          executor.submit(() -> {
            for (int i = 0; i < requestsPerThread; i++) {
              cache.get(keys.get((i + offset) % keys.size()), factory);
            }
          })
        );
      }
      for (var future : futures) {
        future.get(30, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
    }

    var metrics = cache.metrics();
    assertThat(metrics.hitCount() + metrics.missCount()).isEqualTo(
      (long) threads * requestsPerThread
    );
    // Every created value was counted once, whether it stayed in the cache or was evicted
    assertThat(metrics.putCount()).isEqualTo(factory.created.size());
    assertThat(metrics.loadFailureCount()).isEqualTo(0);
    // The bounded part never holds more than the max size, and the retained key is one more
    assertThat(cache.size()).isAtMost(3);
  }

  private static void awaitOrFail(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for the latch");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  /** The stops changed under {@code profile}, by the user preferences of a cache entry. */
  private static Function<AbstractUserPreferences<?>, Set<Integer>> changed(
    WalkPreferences profile,
    int stop
  ) {
    return userPreferences -> profileOf(userPreferences).equals(profile) ? Set.of(stop) : Set.of();
  }

  /** {@link #PROFILE_1} for a reluctance below 3, else {@link #PROFILE_2}. */
  private static WalkPreferences profileOf(AbstractUserPreferences<?> userPreferences) {
    return userPreferences.reluctance().value() < 3 ? PROFILE_1 : PROFILE_2;
  }

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  /**
   * Creates the value {@code "create <preferences>"}, or {@code "patch <preferences> <stops>"},
   * and records it.
   */
  private static final class TestFactory implements RegularTransferServiceCache.Factory<String> {

    private final Runnable onCreate;
    private final List<String> created = new ArrayList<>();
    private final List<Set<Integer>> patched = new ArrayList<>();

    TestFactory() {
      this(() -> {});
    }

    TestFactory(Runnable onCreate) {
      this.onCreate = onCreate;
    }

    @Override
    public String create(AbstractUserPreferences<?> userPreferences) {
      return record(() -> "create " + userPreferences);
    }

    @Override
    public String patch(
      AbstractUserPreferences<?> userPreferences,
      String previous,
      Set<Integer> changedFromStops
    ) {
      patched.add(changedFromStops);
      return record(() -> "patch " + userPreferences + " " + changedFromStops);
    }

    private synchronized String record(Supplier<String> value) {
      onCreate.run();
      var result = value.get();
      created.add(result);
      return result;
    }
  }
}
