package org.opentripplanner.transfer.dataimport;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.RegularTransferPathCounts;

class RegularTransferStatisticsTest {

  private static final FeedScopedId A = id("A");
  private static final FeedScopedId B = id("B");
  private static final FeedScopedId C = id("C");
  private static final FeedScopedId D = id("D");
  private static final RegularTransferPathCounts PATHS = new RegularTransferPathCounts(10, 8, 2);

  @Test
  void countsTheStopsWithTransfersAndTheOnesVisitedByPatterns() {
    // D is not visited by a pattern, C is, but has no transfers
    var subject = RegularTransferStatistics.of(
      List.of(A, B, C, D),
      Set.of(A, B, C)::contains,
      Set.of(A, B),
      PATHS
    );

    assertThat(subject.stops()).isEqualTo(4);
    assertThat(subject.stopsWithTransfers()).isEqualTo(2);
    assertThat(subject.stopsVisitedByPatterns()).isEqualTo(3);
    assertThat(subject.visitedWithoutTransfers()).isEqualTo(1);
  }

  @Test
  void aStopWithTransfersThatIsNotVisitedByAPatternIsNotCountedAsVisited() {
    var subject = RegularTransferStatistics.of(
      List.of(A, D),
      Set.of(A)::contains,
      Set.of(A, D),
      PATHS
    );

    assertThat(subject.stopsWithTransfers()).isEqualTo(2);
    assertThat(subject.stopsVisitedByPatterns()).isEqualTo(1);
    assertThat(subject.visitedWithoutTransfers()).isEqualTo(0);
  }

  @Test
  void theMessageIsOneLineWithThePathsAndTheStops() {
    var subject = new RegularTransferStatistics(
      5_316,
      1_754,
      4_732,
      3_021,
      new RegularTransferPathCounts(38_713, 23_315, 3)
    );

    assertThat(subject.message(Duration.ofMillis(710))).isEqualTo(
      "Regular transfers generated in 710ms: 38 713 paths (23 315 stored, 15 398 reused), 3 profiles. Stops: 5 316, 33% with transfers; 3 021 (64%) of 4 732 visited by patterns have none."
    );
  }

  @Test
  void theMessageHandlesNoStops() {
    var subject = new RegularTransferStatistics(0, 0, 0, 0, new RegularTransferPathCounts(0, 0, 1));

    assertThat(subject.message(Duration.ofSeconds(1))).contains("Stops: 0, 0% with transfers");
  }
}
