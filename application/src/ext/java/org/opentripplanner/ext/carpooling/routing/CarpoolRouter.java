package org.opentripplanner.ext.carpooling.routing;

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
   *         within the implementation's search bound, or the search failed unexpectedly. The
   *         segment's duration is available immediately; its path may be built lazily, see
   *         {@link RoutedSegment#path()}.
   * @throws OTPRequestTimeoutException when the request is cancelled. A cancelled search carries no
   *                                   verdict on whether the leg is routable, so it must propagate
   *                                   instead of being reported as a {@code null} return.
   */
  @Nullable
  RoutedSegment route(Vertex from, Vertex to);
}
