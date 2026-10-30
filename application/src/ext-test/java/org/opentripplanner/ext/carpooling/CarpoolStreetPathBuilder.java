package org.opentripplanner.ext.carpooling;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import org.opentripplanner.ext.carpooling.routing.RoutedSegment;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.search.state.TestStateBuilder;

/**
 * Builder for creating StreetPath objects for carpooling tests using real State chains.
 */
public class CarpoolStreetPathBuilder {

  // Walking speed in m/s (OTP default from WalkPreferences)
  private static final double WALKING_SPEED_MPS = 1.33;

  /**
   * Creates a StreetPath with default 5-minute duration.
   */
  public static StreetPath createStreetPath() {
    return createStreetPath(Duration.ofMinutes(5));
  }

  /**
   * Creates a StreetPath with specified duration using State chain. Uses a single edge with floor
   * distance to avoid rounding errors: the edge traversal applies ceiling when converting to
   * milliseconds, and State.getTime() applies ceiling when converting to seconds, so floor distance
   * ensures the final second-precision duration matches the requested value.
   *
   * @param duration Total duration for the path
   * @return StreetPath with real State objects and accurate timing
   */
  public static StreetPath createStreetPath(Duration duration) {
    var builder = TestStateBuilder.ofWalking();

    int distanceMeters = (int) (duration.toSeconds() * WALKING_SPEED_MPS);

    builder.streetEdge("segment-0", distanceMeters);

    return new StreetPath(builder.build());
  }

  /** A driver route segment of the given duration, wrapping {@link #createStreetPath(Duration)}. */
  public static RoutedSegment createSegment(Duration duration) {
    return RoutedSegment.of(createStreetPath(duration));
  }

  /** {@link #createStreetPaths(int)} wrapped as driver route segments. */
  public static List<RoutedSegment> createSegments(int count) {
    return createStreetPaths(count).stream().map(RoutedSegment::of).toList();
  }

  /**
   * Creates multiple StreetPaths with varying durations. Each path has duration = 5 minutes + index
   * minutes.
   *
   * @param count Number of paths to create
   * @return List of StreetPaths with incrementing durations
   */
  public static List<StreetPath> createStreetPaths(int count) {
    return IntStream.range(0, count)
      .mapToObj(i -> createStreetPath(Duration.ofMinutes(5 + i)))
      .toList();
  }
}
