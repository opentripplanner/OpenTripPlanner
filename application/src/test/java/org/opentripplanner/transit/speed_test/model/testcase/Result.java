package org.opentripplanner.transit.speed_test.model.testcase;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.collection.CompositeComparator;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * This class is responsible for holding information about a test result - a single itinerary. The
 * result can be expected or actual, both represented by this class.
 *
 * @param agencies Alphabetical distinct list of agencies. A {@code List} is used because the order
 *               is important.
 * @param modes Alphabetical distinct list of modes. A {@code List} is used because the order is
 *              important.
 * @param routes A list of routes in tha same order as they appear in the journey.
 * @param stops A list of stops in tha same order as they appear in the journey.
 * @param details Summary description of the journey, like: "Walk 2m ~ Stop A ~ Route L1 12:00 -
 *                12:30 ~ Stop B ~ Walk 3m"
 */
public record Result(
  String testCaseId,
  Integer nTransfers,
  Duration duration,
  Integer cost,
  Integer walkDistance,
  Integer startTime,
  Integer endTime,

  List<String> agencies,
  List<TransitMode> modes,
  List<String> routes,
  List<String> stops,
  String details
) {
  public Result {
    agencies = sortedList(agencies);
    modes = sortedModes(modes);
    routes = List.copyOf(routes);
    stops = List.copyOf(stops);
  }

  public static Comparator<Result> comparator(boolean skipCost) {
    return new CompositeComparator<>(
      Comparator.comparing(r -> r.endTime),
      Comparator.comparing(r -> -r.startTime),
      compareCost(skipCost),
      (r1, r2) -> compare(r1.routes, r2.routes, String::compareTo),
      (r1, r2) -> compare(r1.stops, r2.stops, String::compareTo)
    );
  }

  /** Create a compact String representation of an itinerary. */
  @Override
  public String toString() {
    return String.format(
      "%d %s %d %dm %s %s -- %s",
      nTransfers,
      durationAsStr(),
      cost,
      walkDistance,
      TimeUtils.timeToStrCompact(startTime),
      TimeUtils.timeToStrCompact(endTime),
      details
    );
  }

  public String durationAsStr() {
    return DurationUtils.durationToStr(duration);
  }

  static <T> int compare(List<T> a, List<T> b, Comparator<T> comparator) {
    int size = Math.min(a.size(), b.size());
    for (int i = 0; i < size; i++) {
      int c = comparator.compare(a.get(i), b.get(i));
      if (c != 0) {
        return c;
      }
    }
    return a.size() - b.size();
  }

  private static Comparator<Result> compareCost(boolean skipCost) {
    return (r1, r2) -> {
      if (skipCost) {
        return 0;
      }
      if (r1.cost == null || r1.cost.equals(0)) {
        return 0;
      }
      if (r2.cost == null || r2.cost.equals(0)) {
        return 0;
      }
      return -(r2.cost - r1.cost);
    };
  }

  private static <T> List<T> sortedList(Collection<T> values) {
    return values.stream().sorted().distinct().toList();
  }

  private static List<TransitMode> sortedModes(Collection<TransitMode> modes) {
    return modes.stream().sorted(Comparator.comparing(Enum::name)).distinct().toList();
  }
}
