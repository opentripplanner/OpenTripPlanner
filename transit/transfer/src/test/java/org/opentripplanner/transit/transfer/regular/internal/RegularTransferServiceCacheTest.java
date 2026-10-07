package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType.WALK;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.transit.transfer.regular.RaptorRegularTransferService;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;

class RegularTransferServiceCacheTest {

  private static final WalkPreferences PREFS_1 = withReluctance(1.0);
  private static final WalkPreferences PREFS_2 = withReluctance(2.0);
  private static final WalkPreferences PREFS_3 = withReluctance(3.0);

  @Test
  void buildsOnceAndReturnsTheCachedService() {
    var cache = new RegularTransferServiceCache();
    var builds = new AtomicInteger();

    var first = cache.getOrCreate(WALK, PREFS_1, () -> countedService(builds));
    var second = cache.getOrCreate(WALK, withReluctance(1.0), () -> countedService(builds));

    assertThat(second).isSameInstanceAs(first);
    assertThat(builds.get()).isEqualTo(1);
  }

  @Test
  void differentPreferencesAreCachedSeparately() {
    var cache = new RegularTransferServiceCache();

    var first = cache.getOrCreate(WALK, PREFS_1, EmptyService::new);
    var second = cache.getOrCreate(WALK, PREFS_2, EmptyService::new);

    assertThat(second).isNotSameInstanceAs(first);
  }

  @Test
  void evictsTheLeastRecentlyUsedService() {
    var cache = new RegularTransferServiceCache(2);
    var service1 = cache.getOrCreate(WALK, PREFS_1, EmptyService::new);
    var service2 = cache.getOrCreate(WALK, PREFS_2, EmptyService::new);

    // Use 1, so 2 becomes the least recently used and is evicted when 3 is added
    cache.getOrCreate(WALK, PREFS_1, EmptyService::new);
    cache.getOrCreate(WALK, PREFS_3, EmptyService::new);

    assertThat(cache.getOrCreate(WALK, PREFS_1, EmptyService::new)).isSameInstanceAs(service1);
    assertThat(cache.getOrCreate(WALK, PREFS_2, EmptyService::new)).isNotSameInstanceAs(service2);
  }

  @Test
  void aHitDoesNotWaitForTheBuildOfAnotherKey() throws Exception {
    var cache = new RegularTransferServiceCache();
    var cached = cache.getOrCreate(WALK, PREFS_2, EmptyService::new);
    var buildStarted = new CountDownLatch(1);
    var releaseBuild = new CountDownLatch(1);

    var executor = Executors.newSingleThreadExecutor();
    try {
      var slowBuild = executor.submit(() ->
        cache.getOrCreate(WALK, PREFS_1, () -> {
          buildStarted.countDown();
          awaitOrFail(releaseBuild);
          return new EmptyService();
        })
      );
      awaitOrFail(buildStarted);

      // The build of PREFS_1 is still running, the cached PREFS_2 must be returned anyway
      var hit = assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
        cache.getOrCreate(WALK, PREFS_2, EmptyService::new)
      );
      assertThat(hit).isSameInstanceAs(cached);

      releaseBuild.countDown();
      slowBuild.get(5, TimeUnit.SECONDS);
    } finally {
      releaseBuild.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentMissesOnTheSameKeyReturnTheSameService() throws Exception {
    var cache = new RegularTransferServiceCache();
    var builds = new AtomicInteger();
    var bothBuilding = new CountDownLatch(2);
    Supplier<RaptorRegularTransferService> slowFactory = () -> {
      builds.incrementAndGet();
      bothBuilding.countDown();
      awaitOrFail(bothBuilding);
      return new EmptyService();
    };

    var executor = Executors.newFixedThreadPool(2);
    try {
      var first = executor.submit(() -> cache.getOrCreate(WALK, PREFS_1, slowFactory));
      var second = executor.submit(() -> cache.getOrCreate(WALK, PREFS_1, slowFactory));

      // Both missed and built, the first published service is kept and returned to both
      assertThat(second.get(5, TimeUnit.SECONDS)).isSameInstanceAs(first.get(5, TimeUnit.SECONDS));
      assertThat(builds.get()).isEqualTo(2);
      assertThat(cache.getOrCreate(WALK, PREFS_1, EmptyService::new)).isSameInstanceAs(first.get());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void aFailedBuildIsRetried() {
    var cache = new RegularTransferServiceCache();

    var ex = assertThrows(IllegalStateException.class, () ->
      cache.getOrCreate(WALK, PREFS_1, () -> {
        throw new IllegalStateException("Expected failure");
      })
    );
    assertThat(ex).hasMessageThat().isEqualTo("Expected failure");

    var service = new EmptyService();
    assertThat(cache.getOrCreate(WALK, PREFS_1, () -> service)).isSameInstanceAs(service);
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

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  private static RaptorRegularTransferService countedService(AtomicInteger builds) {
    builds.incrementAndGet();
    return new EmptyService();
  }

  private static class EmptyService implements RaptorRegularTransferService {

    @Override
    public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
      return Collections.emptyIterator();
    }

    @Override
    public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
      return Collections.emptyIterator();
    }
  }
}
