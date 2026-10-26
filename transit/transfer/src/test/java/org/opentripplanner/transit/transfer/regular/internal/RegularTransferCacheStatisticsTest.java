package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.TransferCacheReportEntry;
import org.opentripplanner.transit.transfer.regular.api.TransferCacheReportEntry.Kind;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

class RegularTransferCacheStatisticsTest {

  private static final RegularTransferCacheStatistics.KeyInfo ON_DEMAND =
    new RegularTransferCacheStatistics.KeyInfo(Kind.ON_DEMAND, null, "walk");

  private static AbstractUserPreferences<?> key(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  private static List<Long> hits(RegularTransferCacheStatistics subject) {
    return subject
      .entries(k -> false)
      .stream()
      .map(TransferCacheReportEntry::hits)
      .toList();
  }

  @Test
  void theRequestsOfAKeyAreCountedAndTheMostUsedKeyIsFirst() {
    var subject = RegularTransferCacheStatistics.withCapacity(10);

    subject.record(key(2.0), () -> ON_DEMAND);
    subject.record(key(3.0), () -> ON_DEMAND);
    subject.record(key(3.0), () -> ON_DEMAND);
    subject.record(key(3.0), () -> ON_DEMAND);
    subject.record(key(2.0), () -> ON_DEMAND);

    assertThat(hits(subject)).containsExactly(3L, 2L).inOrder();
  }

  @Test
  void theInformationOfAKeyIsOnlyCreatedTheFirstTime() {
    var subject = RegularTransferCacheStatistics.withCapacity(10);
    var created = new AtomicInteger();

    for (int i = 0; i < 3; ++i) {
      subject.record(key(2.0), () -> {
        created.incrementAndGet();
        return ON_DEMAND;
      });
    }

    assertThat(created.get()).isEqualTo(1);
  }

  @Test
  void theEntryHasTheInformationOfTheKeyAndWhetherItIsCached() {
    var subject = RegularTransferCacheStatistics.withCapacity(10);
    var cachedKey = key(2.0);
    var otherKey = key(3.0);
    subject.record(cachedKey, () ->
      new RegularTransferCacheStatistics.KeyInfo(Kind.PROFILE, "walk", "walk")
    );
    subject.record(otherKey, () -> ON_DEMAND);

    var entries = subject.entries(cachedKey::equals);

    var profile = entries
      .stream()
      .filter(e -> e.kind() == Kind.PROFILE)
      .findFirst()
      .orElseThrow();
    assertThat(profile.id()).isEqualTo("walk");
    assertThat(profile.closestProfileId()).isEqualTo("walk");
    assertThat(profile.cached()).isTrue();
    var onDemand = entries
      .stream()
      .filter(e -> e.kind() == Kind.ON_DEMAND)
      .findFirst()
      .orElseThrow();
    assertThat(onDemand.id()).isNull();
    assertThat(onDemand.closestProfileId()).isEqualTo("walk");
    assertThat(onDemand.cached()).isFalse();
  }

  @Test
  void whenTheLimitIsReachedTheKeysWithOneHitAreDropped() {
    var subject = RegularTransferCacheStatistics.withCapacity(3);
    // Two hits for the first key, one for the others
    subject.record(key(1.0), () -> ON_DEMAND);
    subject.record(key(1.0), () -> ON_DEMAND);
    subject.record(key(2.0), () -> ON_DEMAND);
    subject.record(key(3.0), () -> ON_DEMAND);

    subject.record(key(4.0), () -> ON_DEMAND);

    // The keys with one hit are dropped, the new key has one hit
    assertThat(hits(subject)).containsExactly(2L, 1L).inOrder();
    assertThat(subject.entries(k -> false)).hasSize(2);
  }

  @Test
  void ifNoKeyHasOneHitTheKeysWithTwoHitsOrLessAreDropped() {
    var subject = RegularTransferCacheStatistics.withCapacity(3);
    for (int hits = 0; hits < 2; ++hits) {
      subject.record(key(1.0), () -> ON_DEMAND);
      subject.record(key(2.0), () -> ON_DEMAND);
    }
    for (int hits = 0; hits < 3; ++hits) {
      subject.record(key(3.0), () -> ON_DEMAND);
    }

    subject.record(key(4.0), () -> ON_DEMAND);

    // The keys with two hits are dropped, the key with three hits is kept
    assertThat(hits(subject)).containsExactly(3L, 1L).inOrder();
  }

  @Test
  void theNumberOfKeysIsNeverAboveTheLimit() {
    var subject = RegularTransferCacheStatistics.withCapacity(5);

    for (int i = 1; i <= 200; ++i) {
      subject.record(key(i * 0.5), () -> ON_DEMAND);
      assertThat(subject.entries(k -> false).size()).isAtMost(5);
    }
  }

  @Test
  void theLimitIsThreeTimesTheCacheSizeButAtLeastOneHundred() {
    assertThat(RegularTransferCacheStatistics.MIN_CAPACITY).isEqualTo(100);
    assertThat(RegularTransferCacheStatistics.CACHE_SIZE_FACTOR).isEqualTo(3);
  }

  @Test
  void noStatisticsAreCollectedByTheNoneStatistics() {
    var created = new AtomicInteger();

    RegularTransferCacheStatistics.NONE.record(key(2.0), () -> {
      created.incrementAndGet();
      return ON_DEMAND;
    });

    assertThat(created.get()).isEqualTo(0);
    assertThat(RegularTransferCacheStatistics.NONE.entries(k -> true)).isEmpty();
  }

  @Test
  void theRequestsOfManyThreadsAreAllCounted() throws InterruptedException {
    var subject = RegularTransferCacheStatistics.withCapacity(10);
    var threads = new ArrayList<Thread>();
    for (int t = 0; t < 8; ++t) {
      var thread = new Thread(() -> {
        for (int i = 0; i < 1000; ++i) {
          subject.record(key(2.0), () -> ON_DEMAND);
        }
      });
      threads.add(thread);
      thread.start();
    }
    for (var thread : threads) {
      thread.join();
    }

    assertThat(hits(subject)).containsExactly(8000L);
  }
}
