package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
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
}
