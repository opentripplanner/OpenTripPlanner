package org.opentripplanner.transfer.dataimport;

import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.RegularTransferPathCounts;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * Statistics of the regular transfers generated at graph build, logged as one line when the
 * generation is done.
 *
 * @param stops                    all the regular stops
 * @param stopsWithTransfers       the stops that are the start or the end of at least one transfer
 * @param stopsVisitedByPatterns   the stops that are visited by at least one scheduled pattern
 * @param visitedWithoutTransfers  the stops visited by a pattern that have no transfers
 * @param paths                    the number of paths of the profiles
 */
record RegularTransferStatistics(
  int stops,
  int stopsWithTransfers,
  int stopsVisitedByPatterns,
  int visitedWithoutTransfers,
  RegularTransferPathCounts paths
) {
  /**
   * @param stopsWithTransfers the stops that are the start or the end of at least one transfer
   */
  static RegularTransferStatistics of(
    Collection<FeedScopedId> stopIds,
    Predicate<FeedScopedId> isVisitedByPatterns,
    Set<FeedScopedId> stopsWithTransfers,
    RegularTransferPathCounts paths
  ) {
    int withTransfers = 0;
    int visited = 0;
    int visitedWithoutTransfers = 0;
    for (var stopId : stopIds) {
      boolean hasTransfers = stopsWithTransfers.contains(stopId);
      if (hasTransfers) {
        ++withTransfers;
      }
      if (isVisitedByPatterns.test(stopId)) {
        ++visited;
        if (!hasTransfers) {
          ++visitedWithoutTransfers;
        }
      }
    }
    return new RegularTransferStatistics(
      stopIds.size(),
      withTransfers,
      visited,
      visitedWithoutTransfers,
      paths
    );
  }

  String message(Duration time) {
    return "Regular transfers generated in %s: %s paths (%s stored, %s reused), %d profiles. Stops: %s, %d%% with transfers; %s (%d%%) of %s visited by patterns have none.".formatted(
      DurationUtils.durationToStr(time),
      format(paths.paths()),
      format(paths.stored()),
      format(paths.reused()),
      paths.profiles(),
      format(stops),
      percentage(stopsWithTransfers, stops),
      format(visitedWithoutTransfers),
      percentage(visitedWithoutTransfers, stopsVisitedByPatterns),
      format(stopsVisitedByPatterns)
    );
  }

  private static long percentage(long part, long total) {
    return total == 0 ? 0 : Math.round((100.0 * part) / total);
  }

  /** A number with a space between each group of three digits, for example 1 234 567. */
  private static String format(long number) {
    return String.format(Locale.ROOT, "%,d", number).replace(',', ' ');
  }
}
