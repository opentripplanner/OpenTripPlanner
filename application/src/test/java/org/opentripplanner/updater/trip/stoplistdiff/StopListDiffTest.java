package org.opentripplanner.updater.trip.stoplistdiff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.updater.trip.stoplistdiff.StopListDiffEntry.Added;
import org.opentripplanner.updater.trip.stoplistdiff.StopListDiffEntry.Kept;
import org.opentripplanner.updater.trip.stoplistdiff.StopListDiffEntry.Removed;

class StopListDiffTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final Station STATION_X = TEST_MODEL.station("X").build();

  private static final RegularStop A = TEST_MODEL.stop("A").build();
  private static final RegularStop B = TEST_MODEL.stop("B").build();
  private static final RegularStop C = TEST_MODEL.stop("C").build();
  private static final RegularStop D = TEST_MODEL.stop("D").build();
  private static final RegularStop E = TEST_MODEL.stop("E").build();
  private static final RegularStop X_1 = TEST_MODEL.stop("X_1")
    .withParentStation(STATION_X)
    .build();
  private static final RegularStop X_2 = TEST_MODEL.stop("X_2")
    .withParentStation(STATION_X)
    .build();

  @Test
  void replacedStopInTheMiddle() {
    assertEquals(
      List.of(new Kept(0, 0), new Removed(1), new Added(1), new Kept(2, 2)),
      StopListDiff.diff(comparands(A, B, C), comparands(A, D, C))
    );
  }

  @Test
  void identical() {
    assertEquals(
      List.of(new Kept(0, 0), new Kept(1, 1), new Kept(2, 2)),
      StopListDiff.diff(comparands(A, B, C), comparands(A, B, C))
    );
  }

  @Test
  void addedAndRemovedAtTheEnds() {
    assertEquals(
      List.of(new Added(0), new Kept(0, 1), new Kept(1, 2), new Removed(2)),
      StopListDiff.diff(comparands(A, B, C), comparands(D, A, B))
    );
  }

  @Test
  void nothingInCommon() {
    assertEquals(
      List.of(new Removed(0), new Removed(1), new Added(0), new Added(1)),
      StopListDiff.diff(comparands(A, B), comparands(C, D))
    );
  }

  @Test
  void emptyLists() {
    assertEquals(List.of(new Added(0)), StopListDiff.diff(List.of(), comparands(A)));
    assertEquals(List.of(new Removed(0)), StopListDiff.diff(comparands(A), List.of()));
    assertEquals(List.of(), StopListDiff.diff(List.of(), List.of()));
  }

  @Test
  void loopTrip() {
    assertEquals(
      List.of(new Kept(0, 0), new Removed(1), new Added(1), new Kept(2, 2)),
      StopListDiff.diff(comparands(A, B, A), comparands(A, C, A))
    );
  }

  @Test
  void repeatedStopWithoutTimesMatchesEarliestOccurrence() {
    assertEquals(
      List.of(new Kept(0, 0), new Kept(1, 1), new Removed(2), new Removed(3), new Kept(4, 2)),
      StopListDiff.diff(comparands(A, B, C, B, D), comparands(A, B, D))
    );
  }

  @Test
  void repeatedStopIsResolvedByTime() {
    var original = List.of(
      new StopListDiffComparand(A, OptionalLong.of(100), OptionalLong.empty()),
      new StopListDiffComparand(B, OptionalLong.of(200), OptionalLong.empty()),
      new StopListDiffComparand(C, OptionalLong.of(300), OptionalLong.empty()),
      new StopListDiffComparand(B, OptionalLong.of(400), OptionalLong.empty()),
      new StopListDiffComparand(D, OptionalLong.of(500), OptionalLong.empty())
    );
    // The replacement B is scheduled close to the second B of the original trip
    var replacement = List.of(
      new StopListDiffComparand(A, OptionalLong.of(100), OptionalLong.empty()),
      new StopListDiffComparand(B, OptionalLong.of(410), OptionalLong.empty()),
      new StopListDiffComparand(D, OptionalLong.of(500), OptionalLong.empty())
    );

    assertEquals(
      List.of(new Kept(0, 0), new Removed(1), new Removed(2), new Kept(3, 1), new Kept(4, 2)),
      StopListDiff.diff(original, replacement)
    );
  }

  @Test
  void timeDoesNotReduceTheNumberOfKeptStops() {
    // Matching the far away B keeps C as well, which is preferred over a close time match
    var original = List.of(
      new StopListDiffComparand(B, OptionalLong.of(100), OptionalLong.empty()),
      new StopListDiffComparand(C, OptionalLong.of(200), OptionalLong.empty()),
      new StopListDiffComparand(B, OptionalLong.of(300), OptionalLong.empty())
    );
    var replacement = List.of(
      new StopListDiffComparand(B, OptionalLong.of(290), OptionalLong.empty()),
      new StopListDiffComparand(C, OptionalLong.of(300), OptionalLong.empty())
    );

    assertEquals(
      List.of(new Kept(0, 0), new Kept(1, 1), new Removed(2)),
      StopListDiff.diff(original, replacement)
    );
  }

  @Test
  void platformChangeWithinStationIsKept() {
    assertEquals(
      List.of(new Kept(0, 0), new Kept(1, 1), new Kept(2, 2)),
      StopListDiff.diff(comparands(A, X_1, C), comparands(A, X_2, C))
    );
  }

  @Test
  void stopsWithoutStationAreNotMatchedByStation() {
    assertEquals(
      List.of(new Removed(0), new Added(0)),
      StopListDiff.diff(comparands(A), comparands(B))
    );
  }

  @Test
  void removedAndAddedStopsAreOrderedByArrival() {
    var original = List.of(
      new StopListDiffComparand(A, OptionalLong.of(0), OptionalLong.empty()),
      new StopListDiffComparand(B, OptionalLong.of(100), OptionalLong.empty()),
      new StopListDiffComparand(C, OptionalLong.of(300), OptionalLong.empty()),
      new StopListDiffComparand(E, OptionalLong.of(500), OptionalLong.empty())
    );
    var replacement = List.of(
      new StopListDiffComparand(A, OptionalLong.of(0), OptionalLong.empty()),
      new StopListDiffComparand(D, OptionalLong.of(200), OptionalLong.empty()),
      new StopListDiffComparand(E, OptionalLong.of(500), OptionalLong.empty())
    );

    assertEquals(
      List.of(new Kept(0, 0), new Removed(1), new Added(1), new Removed(2), new Kept(3, 2)),
      StopListDiff.diff(original, replacement)
    );
  }

  @Test
  void removedStopsGoFirstOnEqualOrMissingTimes() {
    var original = List.of(
      new StopListDiffComparand(A, OptionalLong.of(0), OptionalLong.empty()),
      new StopListDiffComparand(B, OptionalLong.of(100), OptionalLong.empty()),
      new StopListDiffComparand(E, OptionalLong.of(500), OptionalLong.empty())
    );
    var replacementSameTime = List.of(
      new StopListDiffComparand(A, OptionalLong.of(0), OptionalLong.empty()),
      new StopListDiffComparand(D, OptionalLong.of(100), OptionalLong.empty()),
      new StopListDiffComparand(E, OptionalLong.of(500), OptionalLong.empty())
    );
    var replacementNoTime = List.of(
      new StopListDiffComparand(A, OptionalLong.of(0), OptionalLong.empty()),
      new StopListDiffComparand(D, OptionalLong.empty(), OptionalLong.empty()),
      new StopListDiffComparand(E, OptionalLong.of(500), OptionalLong.empty())
    );
    var expected = List.of(new Kept(0, 0), new Removed(1), new Added(1), new Kept(2, 2));

    assertEquals(expected, StopListDiff.diff(original, replacementSameTime));
    assertEquals(expected, StopListDiff.diff(original, replacementNoTime));
  }

  @Test
  void departureOnlyStopIsComparedWithScheduledDeparture() {
    // The first B has a long dwell. Comparing the departure-only replacement B with the scheduled
    // arrivals would match the second B (|100 - 410| > |700 - 410|).
    var original = List.of(
      new StopListDiffComparand(B, OptionalLong.of(100), OptionalLong.of(400)),
      new StopListDiffComparand(C, OptionalLong.of(500), OptionalLong.of(500)),
      new StopListDiffComparand(B, OptionalLong.of(700), OptionalLong.of(700))
    );
    var replacement = List.of(
      new StopListDiffComparand(B, OptionalLong.empty(), OptionalLong.of(410))
    );

    assertEquals(
      List.of(new Kept(0, 0), new Removed(1), new Removed(2)),
      StopListDiff.diff(original, replacement)
    );
  }

  @Test
  void arrivalIsPreferredOverDeparture() {
    // The departure is closer to the first B, but the arrival is compared when both have one
    var original = List.of(
      new StopListDiffComparand(B, OptionalLong.of(100), OptionalLong.of(400)),
      new StopListDiffComparand(C, OptionalLong.of(500), OptionalLong.of(500)),
      new StopListDiffComparand(B, OptionalLong.of(700), OptionalLong.of(700))
    );
    var replacement = List.of(
      new StopListDiffComparand(B, OptionalLong.of(690), OptionalLong.of(410))
    );

    assertEquals(
      List.of(new Removed(0), new Removed(1), new Kept(2, 0)),
      StopListDiff.diff(original, replacement)
    );
  }

  @Test
  void noTimeOfTheSameKindCountsAsNoTime() {
    var original = List.of(
      new StopListDiffComparand(B, OptionalLong.of(100), OptionalLong.empty()),
      new StopListDiffComparand(C, OptionalLong.empty(), OptionalLong.empty()),
      new StopListDiffComparand(B, OptionalLong.of(700), OptionalLong.empty())
    );
    var replacement = List.of(
      new StopListDiffComparand(B, OptionalLong.empty(), OptionalLong.of(700))
    );

    assertEquals(
      List.of(new Kept(0, 0), new Removed(1), new Removed(2)),
      StopListDiff.diff(original, replacement)
    );
  }

  @Test
  void removedAndAddedStopsAreOrderedByDeparture() {
    var original = List.of(
      new StopListDiffComparand(A, OptionalLong.of(0), OptionalLong.empty()),
      new StopListDiffComparand(B, OptionalLong.empty(), OptionalLong.of(100)),
      new StopListDiffComparand(E, OptionalLong.of(500), OptionalLong.empty())
    );
    var replacement = List.of(
      new StopListDiffComparand(A, OptionalLong.of(0), OptionalLong.empty()),
      new StopListDiffComparand(D, OptionalLong.empty(), OptionalLong.of(50)),
      new StopListDiffComparand(E, OptionalLong.of(500), OptionalLong.empty())
    );

    assertEquals(
      List.of(new Kept(0, 0), new Added(1), new Removed(1), new Kept(2, 2)),
      StopListDiff.diff(original, replacement)
    );
  }

  private static List<StopListDiffComparand> comparands(RegularStop... stops) {
    return Arrays.stream(stops)
      .map(s -> new StopListDiffComparand(s, OptionalLong.empty(), OptionalLong.empty()))
      .toList();
  }
}
