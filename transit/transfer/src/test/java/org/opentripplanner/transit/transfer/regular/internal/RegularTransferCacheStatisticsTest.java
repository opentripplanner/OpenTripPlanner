package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.report.TransferCacheReportEntry;
import org.opentripplanner.transit.transfer.regular.api.report.UserPreferenceSource;

class RegularTransferCacheStatisticsTest {

  private static final RegularTransferCacheStatistics.KeyInfo ON_DEMAND =
    new RegularTransferCacheStatistics.KeyInfo(UserPreferenceSource.ON_DEMAND, null, "walk");

  private static AbstractUserPreferences<?> key(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  private static List<Long> hits(RegularTransferCacheStatistics subject) {
    return subject
      .entries(k -> false, k -> ON_DEMAND)
      .stream()
      .map(TransferCacheReportEntry::hits)
      .toList();
  }

  @Test
  void theRequestsOfAKeyAreCountedAndTheMostUsedKeyIsFirst() {
    var subject = RegularTransferCacheStatistics.withCapacity(10);

    subject.record(key(2.0));
    subject.record(key(3.0));
    subject.record(key(3.0));
    subject.record(key(3.0));
    subject.record(key(2.0));

    assertThat(hits(subject)).containsExactly(3L, 2L).inOrder();
  }

  @Test
  void theInformationOfAKeyIsOnlyResolvedWhenTheReportIsBuilt() {
    var subject = RegularTransferCacheStatistics.withCapacity(10);
    var resolved = new AtomicInteger();

    for (int i = 0; i < 3; ++i) {
      subject.record(key(2.0));
    }
    assertThat(resolved.get()).isEqualTo(0);

    subject.entries(
      k -> false,
      k -> {
        resolved.incrementAndGet();
        return ON_DEMAND;
      }
    );

    assertThat(resolved.get()).isEqualTo(1);
  }

  @Test
  void theEntryHasTheInformationOfTheKeyAndWhetherItIsCached() {
    var subject = RegularTransferCacheStatistics.withCapacity(10);
    var cachedKey = key(2.0);
    var otherKey = key(3.0);
    subject.record(cachedKey);
    subject.record(otherKey);

    var entries = subject.entries(cachedKey::equals, k ->
      k.equals(cachedKey)
        ? new RegularTransferCacheStatistics.KeyInfo(UserPreferenceSource.PROFILE, "walk", "walk")
        : ON_DEMAND
    );

    var profile = entries
      .stream()
      .filter(e -> e.source() == UserPreferenceSource.PROFILE)
      .findFirst()
      .orElseThrow();
    assertThat(profile.id()).isEqualTo("walk");
    assertThat(profile.closestProfileId()).isEqualTo("walk");
    assertThat(profile.cached()).isTrue();
    var onDemand = entries
      .stream()
      .filter(e -> e.source() == UserPreferenceSource.ON_DEMAND)
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
    subject.record(key(1.0));
    subject.record(key(1.0));
    subject.record(key(2.0));
    subject.record(key(3.0));

    subject.record(key(4.0));

    // The keys with one hit are dropped, the new key has one hit
    assertThat(hits(subject)).containsExactly(2L, 1L).inOrder();
    assertThat(subject.entries(k -> false, k -> ON_DEMAND)).hasSize(2);
  }

  @Test
  void ifNoKeyHasOneHitTheKeysWithTwoHitsOrLessAreDropped() {
    var subject = RegularTransferCacheStatistics.withCapacity(3);
    for (int hits = 0; hits < 2; ++hits) {
      subject.record(key(1.0));
      subject.record(key(2.0));
    }
    for (int hits = 0; hits < 3; ++hits) {
      subject.record(key(3.0));
    }

    subject.record(key(4.0));

    // The keys with two hits are dropped, the key with three hits is kept
    assertThat(hits(subject)).containsExactly(3L, 1L).inOrder();
  }

  @Test
  void theNumberOfKeysIsNeverAboveTheLimit() {
    var subject = RegularTransferCacheStatistics.withCapacity(5);

    for (int i = 1; i <= 200; ++i) {
      subject.record(key(i * 0.5));
      assertThat(subject.entries(k -> false, k -> ON_DEMAND).size()).isAtMost(5);
    }
  }

  @Test
  void theLimitIsThreeTimesTheCacheSizeButAtLeastOneHundred() {
    assertThat(RegularTransferCacheStatistics.MIN_CAPACITY).isEqualTo(100);
    assertThat(RegularTransferCacheStatistics.CACHE_SIZE_FACTOR).isEqualTo(3);
  }

  @Test
  void noStatisticsAreCollectedByTheNoneStatistics() {
    RegularTransferCacheStatistics.NONE.record(key(2.0));

    assertThat(RegularTransferCacheStatistics.NONE.entries(k -> true, k -> ON_DEMAND)).isEmpty();
  }

  @Test
  void theRequestsOfManyThreadsAreAllCounted() throws InterruptedException {
    var subject = RegularTransferCacheStatistics.withCapacity(10);
    var threads = new ArrayList<Thread>();
    for (int t = 0; t < 8; ++t) {
      var thread = new Thread(() -> {
        for (int i = 0; i < 1000; ++i) {
          subject.record(key(2.0));
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
