package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Functional interface for street routing.
 */
@FunctionalInterface
public interface CarpoolRouter {
  /**
   * Routes in CAR mode from {@code from} to {@code to}.
   *
   * @return the best segment found, or {@code null} when none can be returned: no route exists
   *         within the implementation's search bound, or the search failed unexpectedly.
   * @throws OTPRequestTimeoutException when the request is cancelled. A cancelled search carries no
   *                                    verdict on whether the leg is routable, so it must propagate
   *                                    instead of being reported as a {@code null} return.
   */
  @Nullable
  RoutedSegment route(Vertex from, Vertex to);

  /**
   * Like {@link #route(Vertex, Vertex)}, for a caller that has no use for a segment longer than
   * {@code limit}: the search may stop there and return {@code null}. By default it searches as far
   * as {@link #route(Vertex, Vertex)} does.
   */
  @Nullable
  default RoutedSegment route(Vertex from, Vertex to, Duration limit) {
    return route(from, to);
  }
}
