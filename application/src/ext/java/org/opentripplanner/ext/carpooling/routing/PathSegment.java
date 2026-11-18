package org.opentripplanner.ext.carpooling.routing;

import java.util.Objects;
import org.opentripplanner.street.model.path.StreetPath;

/**
 * A {@link RoutedSegment} over a path that already exists, such as the result of a goal-directed
 * search.
 */
final class PathSegment implements RoutedSegment {

  private final StreetPath path;

  PathSegment(StreetPath path) {
    this.path = Objects.requireNonNull(path, "path");
  }

  @Override
  public int durationSeconds() {
    return (int) path.duration().toSeconds();
  }

  @Override
  public StreetPath path() {
    return path;
  }

  @Override
  public String toString() {
    return (
      "PathSegment{" +
      path.states().getFirst().getVertex() +
      " -> " +
      path.lastState().getVertex() +
      ", " +
      durationSeconds() +
      "s}"
    );
  }
}
