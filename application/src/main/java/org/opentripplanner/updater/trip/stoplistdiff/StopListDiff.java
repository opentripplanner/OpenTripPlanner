package org.opentripplanner.updater.trip.stoplistdiff;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import org.opentripplanner.updater.trip.stoplistdiff.StopListDiffEntry.Added;
import org.opentripplanner.updater.trip.stoplistdiff.StopListDiffEntry.Kept;
import org.opentripplanner.updater.trip.stoplistdiff.StopListDiffEntry.Removed;

/**
 * Computes the difference between the stop list of a scheduled trip and the stop list of a
 * real-time replacement of the same trip (for example a GTFS-RT trip update with schedule
 * relationship {@code REPLACEMENT}).
 * <p>
 * The result classifies every stop of both lists as kept (in both lists), removed (in the scheduled
 * trip only) or added (in the replacement only). The entries are returned in travel order.
 * <p>
 * The alignment is a longest common subsequence: as many stops as possible are kept. Two stops
 * match if they are the same stop or belong to the same parent station, so a platform change is
 * treated as a kept stop. Trips may visit the same stop several times (e.g. loops); if several
 * alignments keep the same number of stops, the one with the smallest total difference between
 * the scheduled times of the matched stops wins. Times are compared like for like: arrival with
 * arrival, or departure with departure if not both stops have an arrival. If no times are
 * available, the earliest possible occurrence in the scheduled trip is matched.
 * <p>
 * Between two kept stops, removed and added stops are ordered by their scheduled times, compared in
 * the same way. If there is no time of the same kind or the times are equal, removed stops come
 * before added stops.
 */
public final class StopListDiff {

  private StopListDiff() {}

  /**
   * Compute the diff between the {@code original} and the {@code replacement} stop lists.
   */
  public static List<StopListDiffEntry> diff(
    List<StopListDiffComparand> original,
    List<StopListDiffComparand> replacement
  ) {
    int n = original.size();
    int m = replacement.size();

    // Suffix DP: matches[i][j] / cost[i][j] is the best alignment of original[i..] and
    // replacement[j..]. More matches is better; with equal matches, lower cost is better.
    int[][] matches = new int[n + 1][m + 1];
    long[][] cost = new long[n + 1][m + 1];

    for (int i = n - 1; i >= 0; i--) {
      for (int j = m - 1; j >= 0; j--) {
        // Best result without pairing the original[i] and replacement[j] stops.
        int bestMatches = matches[i + 1][j];
        long bestCost = cost[i + 1][j];
        if (isBetter(matches[i][j + 1], cost[i][j + 1], bestMatches, bestCost)) {
          bestMatches = matches[i][j + 1];
          bestCost = cost[i][j + 1];
        }

        // Pair the two stops instead if they match and the result is better. Pairing does not
        // always win: a later occurrence of the same stop may keep as many stops with a smaller
        // time difference.
        if (isMatch(original.get(i), replacement.get(j))) {
          int candidateMatches = matches[i + 1][j + 1] + 1;
          long candidateCost =
            cost[i + 1][j + 1] + timeDifference(original.get(i), replacement.get(j));
          if (isBetter(candidateMatches, candidateCost, bestMatches, bestCost)) {
            bestMatches = candidateMatches;
            bestCost = candidateCost;
          }
        }

        matches[i][j] = bestMatches;
        cost[i][j] = bestCost;
      }
    }

    // Walk forward and collect kept pairs, preferring a match whenever it is optimal. This
    // matches the earliest possible occurrence when alignments are otherwise equal.
    var result = new ArrayList<StopListDiffEntry>(n + m);
    var removed = new ArrayList<Integer>();
    var added = new ArrayList<Integer>();
    int i = 0;
    int j = 0;
    while (i < n && j < m) {
      if (
        isMatch(original.get(i), replacement.get(j)) &&
        matches[i][j] == matches[i + 1][j + 1] + 1 &&
        cost[i][j] == cost[i + 1][j + 1] + timeDifference(original.get(i), replacement.get(j))
      ) {
        flushRemovedAndAdded(original, replacement, removed, added, result);
        result.add(new Kept(i++, j++));
      } else if (matches[i][j] == matches[i + 1][j] && cost[i][j] == cost[i + 1][j]) {
        removed.add(i++);
      } else {
        added.add(j++);
      }
    }

    // One list is exhausted and the rest of the other list is unmatched after the last kept stop.
    while (i < n) {
      removed.add(i++);
    }
    while (j < m) {
      added.add(j++);
    }
    flushRemovedAndAdded(original, replacement, removed, added, result);

    return result;
  }

  private static boolean isBetter(int matchesA, long costA, int matchesB, long costB) {
    return matchesA > matchesB || (matchesA == matchesB && costA < costB);
  }

  private static boolean isMatch(StopListDiffComparand a, StopListDiffComparand b) {
    return a.stop().equals(b.stop()) || a.stop().isPartOfSameStationAs(b.stop());
  }

  /**
   * Merge the removed and added stops between two kept stops in order of their scheduled times.
   * Removed stops go first if a time is missing or the times are equal.
   */
  private static void flushRemovedAndAdded(
    List<StopListDiffComparand> original,
    List<StopListDiffComparand> replacement,
    List<Integer> removed,
    List<Integer> added,
    List<StopListDiffEntry> result
  ) {
    int r = 0;
    int a = 0;
    while (r < removed.size() || a < added.size()) {
      if (r == removed.size()) {
        result.add(new Added(added.get(a++)));
      } else if (a == added.size()) {
        result.add(new Removed(removed.get(r++)));
      } else {
        var difference = signedTimeDifference(
          original.get(removed.get(r)),
          replacement.get(added.get(a))
        );
        if (difference.isPresent() && difference.getAsLong() > 0) {
          result.add(new Added(added.get(a++)));
        } else {
          result.add(new Removed(removed.get(r++)));
        }
      }
    }
    removed.clear();
    added.clear();
  }

  /**
   * The absolute difference between the times of two stops, comparing arrival with arrival, or
   * departure with departure if not both have an arrival. Zero if they have no time of the same
   * kind.
   */
  private static long timeDifference(StopListDiffComparand a, StopListDiffComparand b) {
    if (a.arrivalTime().isPresent() && b.arrivalTime().isPresent()) {
      return Math.abs(a.arrivalTime().getAsLong() - b.arrivalTime().getAsLong());
    }
    if (a.departureTime().isPresent() && b.departureTime().isPresent()) {
      return Math.abs(a.departureTime().getAsLong() - b.departureTime().getAsLong());
    }
    return 0;
  }

  /**
   * The difference a − b between the times of two stops, comparing arrival with arrival, or
   * departure with departure if not both have an arrival. Empty if they have no time of the same
   * kind.
   */
  private static OptionalLong signedTimeDifference(
    StopListDiffComparand a,
    StopListDiffComparand b
  ) {
    if (a.arrivalTime().isPresent() && b.arrivalTime().isPresent()) {
      return OptionalLong.of(a.arrivalTime().getAsLong() - b.arrivalTime().getAsLong());
    }
    if (a.departureTime().isPresent() && b.departureTime().isPresent()) {
      return OptionalLong.of(a.departureTime().getAsLong() - b.departureTime().getAsLong());
    }
    return OptionalLong.empty();
  }
}
