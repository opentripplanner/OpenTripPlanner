package org.opentripplanner.ext.carpooling.util;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

/**
 * Helpers for reading the duration and A* weight of {@link StreetPath} segments, where a missing
 * segment counts as zero, and for replaying a walk with another request's preferences.
 */
public final class StreetPathUtils {

  private StreetPathUtils() {}

  /**
   * Returns the duration of the given path, or {@link Duration#ZERO} if the path is {@code null}.
   */
  public static Duration durationOrZero(@Nullable StreetPath path) {
    return path == null ? Duration.ZERO : path.duration();
  }

  /**
   * Returns the A* weight of the given path, or {@code 0} if the path is {@code null}. The weight
   * already accounts for the user's walk preferences (reluctance, safety factor, slope cost,
   * etc.) since it comes from the search that produced the path.
   */
  public static double weightOrZero(@Nullable StreetPath path) {
    return path == null ? 0 : path.weight();
  }

  /**
   * The same walk timed and weighted with {@code request}'s preferences: drives the path's edges
   * through the real traversals from its first vertex, no search involved. {@code null} when an
   * edge cannot be traversed under the request, for instance by a wheelchair user.
   */
  @Nullable
  public static StreetPath replay(StreetPath path, StreetSearchRequest request) {
    var walkRequest = StreetSearchRequest.copyOf(request)
      .withMode(StreetMode.WALK)
      .withArriveBy(false)
      .build();
    var states = path.states();
    State state = new State(states.getFirst().getVertex(), walkRequest);
    for (State step : states.subList(1, states.size())) {
      Edge edge = step.getBackEdge();
      State next = null;
      for (State candidate : edge.traverse(state)) {
        if (candidate.getVertex() == edge.getToVertex()) {
          next = candidate;
          break;
        }
      }
      if (next == null) {
        return null;
      }
      state = next;
    }
    return new StreetPath(state);
  }
}
