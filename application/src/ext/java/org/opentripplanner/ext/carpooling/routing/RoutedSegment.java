package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.ext.carpooling.util.StreetPathUtils;
import org.opentripplanner.street.model.path.StreetPath;

/**
 * The drive between two consecutive waypoints of a (possibly modified) driver route, as routed by a
 * {@link CarpoolRouter}.
 * <p>
 * Insertion evaluation needs only the duration; the street path is needed for the few insertions
 * that end up in an itinerary. A segment therefore knows its {@link #durationSeconds() duration}
 * from the start, and one that does not hold its {@link #path()} builds it on demand.
 */
public interface RoutedSegment {
  /** Travel time in whole seconds, the same value {@code path().duration().toSeconds()} reports. */
  int durationSeconds();

  default Duration duration() {
    return Duration.ofSeconds(durationSeconds());
  }

  /**
   * The street path in chronological order, built on the first call if the segment does not hold
   * one yet. Never {@code null}.
   */
  StreetPath path();

  /** Wraps an already materialised path. */
  static RoutedSegment of(StreetPath path) {
    return new PathSegment(path);
  }

  /**
   * Cumulative durations along a chain of segments, with {@code stopDuration} added at every
   * intermediate point.
   */
  static Duration[] cumulativeDurations(List<RoutedSegment> segments, Duration stopDuration) {
    Duration[] durations = new Duration[segments.size()];
    for (int i = 0; i < durations.length; i++) {
      durations[i] = segments.get(i).duration();
    }
    return StreetPathUtils.calculateCumulativeDurations(durations, stopDuration);
  }
}
