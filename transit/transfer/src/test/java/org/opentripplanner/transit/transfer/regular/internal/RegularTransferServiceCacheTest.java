package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType.WALK;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
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
