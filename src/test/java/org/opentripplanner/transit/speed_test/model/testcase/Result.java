package org.opentripplanner.transit.speed_test.model.testcase;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.framework.collection.CompositeComparator;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * This class is responsible for holding information about a test result - a single itinerary. The
 * result can be expected or actual, both represented by this class.
 * <p>
 * Implementation details: This is NOT converted into a record, because it is hard to enforce the
 * restrictions on agencies, modes, routes and stops. Second, it is not much simpler/less code.
 */
class Result {

  /**
   * The status is not final; This allows to update the status when matching expected and actual
   * results.
   */
  final String testCaseId;
  final Integer nTransfers;
  final Duration duration;
  final Integer cost;
  final Integer walkDistance;
  final Integer startTime;
  final Integer endTime;

  /** Alphabetical distinct list of agencies. A {@code List} is used because the order is important. */
  final List<String> agencies;
  /** Alphabetical distinct list of modes. A {@code List} is used because the order is important. */
  final List<TransitMode> modes;
  /** A list of routes in tha same order as they appear in the journey. */
  final List<String> routes;
  /** A list of stops in tha same order as they appear in the journey. */
  final List<String> stops;
  /**
   * Summary description of the journey, like: "Walk 2m ~ Stop A ~ Route L1 12:00 - 12:30 ~ Stop B ~
   * Walk 3m"
   */
  final String details;

  Result(
    String testCaseId,
    Integer nTransfers,
    Duration duration,
    Integer cost,
    Integer walkDistance,
    Integer startTime,
    Integer endTime,
    Collection<String> agencies,
    Collection<TransitMode> modes,
    Collection<String> routes,
    Collection<String> stops,
    String details
  ) {
    this.testCaseId = testCaseId;
    this.nTransfers = nTransfers;
    this.duration = duration;
    this.cost = cost;
    this.walkDistance = walkDistance;
    this.startTime = startTime;
    this.endTime = endTime;
    this.agencies = sortedList(agencies);
    this.modes = sortedModes(modes);
    this.routes = List.copyOf(routes);
    this.stops = List.copyOf(stops);
    this.details = details;
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
