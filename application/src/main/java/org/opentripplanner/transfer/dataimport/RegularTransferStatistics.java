package org.opentripplanner.transfer.dataimport;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.RegularTransferProfileStatistics;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * Statistics of the regular transfers generated at graph build, logged when the generation is done:
 * a line for each profile, and a line with the total. Each line has the share of the stops that
 * have no transfers, and the number of trip patterns that are not reached by any transfer: a
 * pattern is reached if at least one of its stops is the start or the end of a transfer. A pattern
 * that is not reached by any transfer of any profile is probably a sign of a problem.
 */
final class RegularTransferStatistics {

  /** The stops and the patterns without transfers, for a profile or for all the profiles. */
  private record NoTransfers(
    int stops,
    int stopsWithoutTransfers,
    int patterns,
    int patternsWithoutTransfers
  ) {
    static NoTransfers of(
      Collection<FeedScopedId> stopIds,
      Collection<? extends Collection<FeedScopedId>> stopsOfEachPattern,
      Set<FeedScopedId> stopsWithTransfers
    ) {
      int stopsWithout = 0;
      for (var stopId : stopIds) {
        if (!stopsWithTransfers.contains(stopId)) {
          ++stopsWithout;
        }
      }
      int patternsWithout = 0;
      for (var patternStops : stopsOfEachPattern) {
        if (patternStops.stream().noneMatch(stopsWithTransfers::contains)) {
          ++patternsWithout;
        }
      }
      return new NoTransfers(
        stopIds.size(),
        stopsWithout,
        stopsOfEachPattern.size(),
        patternsWithout
      );
    }

    /** The patterns are the number and the percentage, so a single pattern is not rounded away. */
    String message() {
      return "No transfers: %d%% stops, %s (%d%%) patterns".formatted(
        percentage(stopsWithoutTransfers, stops),
        format(patternsWithoutTransfers),
        percentage(patternsWithoutTransfers, patterns)
      );
    }
  }

  private final List<RegularTransferProfileStatistics> profiles;
  private final List<NoTransfers> profilesNoTransfers;
  private final NoTransfers totalNoTransfers;

  private RegularTransferStatistics(
    List<RegularTransferProfileStatistics> profiles,
    List<NoTransfers> profilesNoTransfers,
    NoTransfers totalNoTransfers
  ) {
    this.profiles = profiles;
    this.profilesNoTransfers = profilesNoTransfers;
    this.totalNoTransfers = totalNoTransfers;
  }

  /**
   * @param stopsOfEachPattern the stops of each scheduled trip pattern
   * @param stopsWithTransfers the stops that are the start or the end of at least one transfer of
   *                           any profile
   */
  static RegularTransferStatistics of(
    Collection<FeedScopedId> stopIds,
    Collection<? extends Collection<FeedScopedId>> stopsOfEachPattern,
    Set<FeedScopedId> stopsWithTransfers,
    List<RegularTransferProfileStatistics> profiles
  ) {
    var profilesNoTransfers = profiles
      .stream()
      .map(p -> NoTransfers.of(stopIds, stopsOfEachPattern, p.stopsWithTransfers()))
      .toList();
    return new RegularTransferStatistics(
      profiles,
      profilesNoTransfers,
      NoTransfers.of(stopIds, stopsOfEachPattern, stopsWithTransfers)
    );
  }

  /** A line for each profile, in the order of the profiles, and the total as the last line. */
  List<String> messages(Duration time) {
    var result = new ArrayList<String>();
    long uniquePaths = 0;
    for (int i = 0; i < profiles.size(); ++i) {
      var profile = profiles.get(i);
      uniquePaths += profile.stored();
      result.add(profileMessage(profile, profilesNoTransfers.get(i)));
    }
    result.add(
      "Regular transfers generated in %s. %s unique street paths in %d profiles - %s.".formatted(
        DurationUtils.durationToStr(time),
        format(uniquePaths),
        profiles.size(),
        totalNoTransfers.message()
      )
    );
    return result;
  }

  private static String profileMessage(
    RegularTransferProfileStatistics profile,
    NoTransfers noTransfers
  ) {
    var message = "Profile '%s'. Street paths: %s".formatted(profile.id(), format(profile.paths()));
    if (profile.parentId() != null) {
      message += ", %d%% reused '%s'".formatted(
        percentage(profile.reused(), profile.paths()),
        profile.parentId()
      );
    }
    return message + " - %s - %s.".formatted(noTransfers.message(), profile.preferences());
  }

  private static long percentage(long part, long total) {
    return total == 0 ? 0 : Math.round((100.0 * part) / total);
  }

  /** A number with a space between each group of three digits, for example 1 234 567. */
  private static String format(long number) {
    return String.format(Locale.ROOT, "%,d", number).replace(',', ' ');
  }
}
