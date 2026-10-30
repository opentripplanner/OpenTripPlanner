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

  private static void record(
    RegularTransferCacheStatistics subject,
    AbstractUserPreferences<?> key,
    int times
  ) {
    for (int i = 0; i < times; ++i) {
      subject.record(key);
    }
  }

  private static boolean isTracked(
    RegularTransferCacheStatistics subject,
    AbstractUserPreferences<?> key
  ) {
    return subject
      .entries(k -> false, k -> ON_DEMAND)
      .stream()
      .anyMatch(entry -> entry.userPreferences().equals(key.toString()));
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
  void whenTheLimitIsReachedTheKeyWithTheFewestHitsIsDropped() {
    var subject = RegularTransferCacheStatistics.withCapacity(3);
    record(subject, key(1.0), 3);
    record(subject, key(2.0), 1);
    record(subject, key(3.0), 2);

    subject.record(key(4.0));

    // Only the key with one hit is dropped, the new key has one hit
    assertThat(hits(subject)).containsExactly(3L, 2L, 1L).inOrder();
    assertThat(isTracked(subject, key(2.0))).isFalse();
    assertThat(isTracked(subject, key(4.0))).isTrue();
  }

  @Test
  void ifAllKeysHaveManyHitsOnlyTheKeyWithTheFewestHitsIsDropped() {
    var subject = RegularTransferCacheStatistics.withCapacity(3);
    record(subject, key(1.0), 10_000);
    record(subject, key(2.0), 9_999);
    record(subject, key(3.0), 10_001);

    subject.record(key(4.0));

    assertThat(hits(subject)).containsExactly(10_001L, 10_000L, 1L).inOrder();
    assertThat(isTracked(subject, key(2.0))).isFalse();
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
