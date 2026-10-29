package org.opentripplanner.transfer.dataimport;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters.DEFAULT_REUSE_LIMIT;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.RegularTransferProfileStatistics;

class RegularTransferStatisticsTest {

  private static final List<FeedScopedId> STOPS = stops(0, 100);

  /**
   * 88 stops have transfers: the stops 0-78 and 80-88. The other 12 stops have none.
   */
  private static final Set<FeedScopedId> WITH_TRANSFERS = withTransfers();

  /**
   * Four patterns. The patterns B and D have no stop with transfers. Stop 79 has no transfers, but
   * the stop 80 of the pattern C has.
   */
  private static final Collection<List<FeedScopedId>> PATTERNS = List.of(
    stops(0, 2),
    List.of(id("S79"), id("S95")),
    List.of(id("S79"), id("S80")),
    stops(90, 92)
  );

  @Test
  void aProfileWithoutAParentHasItsStreetPathsAndTheStopsAndPatternsWithoutTransfers() {
    var walk = profile("walk", "WalkPreferences{}", null, 555_110, 0, WITH_TRANSFERS);

    var subject = statistics(WITH_TRANSFERS, walk);

    assertThat(subject.messages(Duration.ofSeconds(1)).getFirst()).isEqualTo(
      "Profile 'walk'. Street paths: 555 110 - No transfers: 12% stops, 2 (50%) patterns - WalkPreferences{}."
    );
  }

  @Test
  void aProfileWithAParentHasThePercentageOfTheReusedPaths() {
    var wheelchair = profile(
      "wheelchair",
      "WheelchairPreferences{}",
      "walk",
      569_979,
      308_529,
      WITH_TRANSFERS
    );

    var subject = statistics(WITH_TRANSFERS, wheelchair);

    assertThat(subject.messages(Duration.ofSeconds(1)).getFirst()).isEqualTo(
      "Profile 'wheelchair'. Street paths: 569 979, 54% reused 'walk' (15s + 2%) - No transfers: 12% stops, 2 (50%) patterns - WheelchairPreferences{}."
    );
  }

  @Test
  void theNoTransfersOfAProfileAreCountedForTheStopsOfThatProfile() {
    // The profile only has transfers at the first 50 stops, so only the pattern A is reached
    var few = profile("car", "CarPreferences{}", null, 10, 0, new HashSet<>(stops(0, 50)));

    var subject = statistics(WITH_TRANSFERS, few);

    assertThat(subject.messages(Duration.ofSeconds(1)).getFirst()).contains(
      "No transfers: 50% stops, 3 (75%) patterns"
    );
  }

  @Test
  void aPatternThatIsReachedThroughOneOfItsStopsHasTransfers() {
    // Only the stop 80 has transfers: it reaches the pattern C, and no other pattern
    var only80 = profile("walk", "w", null, 1, 0, Set.of(id("S80")));

    var subject = statistics(Set.of(id("S80")), only80);

    assertThat(subject.messages(Duration.ofSeconds(1)).getFirst()).contains("3 (75%) patterns");
  }

  @Test
  void theTotalHasTheTimeTheUniqueStreetPathsAndTheStopsAndPatternsWithoutTransfersOfAllProfiles() {
    var walk = profile("walk", "w", null, 555_110, 0, WITH_TRANSFERS);
    var wheelchair = profile("wheelchair", "c", "walk", 569_979, 308_529, WITH_TRANSFERS);

    var subject = statistics(WITH_TRANSFERS, walk, wheelchair);

    var messages = subject.messages(Duration.ofMillis(28_951));
    assertThat(messages).hasSize(3);
    // 555 110 + (569 979 - 308 529) street paths are stored
    assertThat(messages.getLast()).isEqualTo(
      "Regular transfers generated in 28s951ms. 816 560 unique street paths in 2 profiles - No transfers: 12% stops, 2 (50%) patterns."
    );
  }

  @Test
  void noStopsAndNoPatternsGiveZero() {
    var walk = profile("walk", "w", null, 0, 0, Set.of());

    var subject = RegularTransferStatistics.of(List.of(), List.of(), Set.of(), List.of(walk));

    assertThat(subject.messages(Duration.ofSeconds(1)).getLast()).contains(
      "No transfers: 0% stops, 0 (0%) patterns"
    );
  }

  private static RegularTransferStatistics statistics(
    Set<FeedScopedId> allWithTransfers,
    RegularTransferProfileStatistics... profiles
  ) {
    return RegularTransferStatistics.of(STOPS, PATTERNS, allWithTransfers, List.of(profiles));
  }

  private static RegularTransferProfileStatistics profile(
    String id,
    String preferences,
    String parent,
    long paths,
    long reused,
    Set<FeedScopedId> stopsWithTransfers
  ) {
    return new RegularTransferProfileStatistics(
      id,
      preferences,
      DEFAULT_REUSE_LIMIT,
      parent,
      paths,
      reused,
      stopsWithTransfers
    );
  }

  private static List<FeedScopedId> stops(int fromInclusive, int toExclusive) {
    var result = new ArrayList<FeedScopedId>();
    for (int i = fromInclusive; i < toExclusive; ++i) {
      result.add(id("S" + i));
    }
    return result;
  }

  private static Set<FeedScopedId> withTransfers() {
    var result = new HashSet<FeedScopedId>(stops(0, 79));
    result.addAll(stops(80, 89));
    return result;
  }
}
