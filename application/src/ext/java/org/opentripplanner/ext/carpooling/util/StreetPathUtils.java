package org.opentripplanner.ext.carpooling.util;

import java.time.Duration;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.api.path.StreetPath;
import org.opentripplanner.street.api.path.StreetPathStates;
import org.opentripplanner.street.api.request.StreetSearchRequest;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.state.State;

/**
 * Helpers for reading the duration and A* weight of {@link StreetPath} segments, where a missing
 * segment counts as zero, for replaying a walk with another request's preferences, and for reading
 * the geometry and vertices of paths.
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
   * already accounts for the user's walk preferences (reluctance, safety factor, slope cost, etc.)
   * since it comes from the search that produced the path.
   */
  public static double weightOrZero(@Nullable StreetPath path) {
    return path == null ? 0 : StreetPathStates.states(path).getLast().getWeight();
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
    var edges = edges(List.of(path));
    return EdgeTraverser.createPath(edges, walkRequest).orElse(null);
  }

  /**
   * The geometry of all the given paths concatenated.
   */
  public static LineString geometry(List<StreetPath> paths) {
    return GeometryUtils.concatenateLineStrings(edges(paths), Edge::getGeometry);
  }

  /**
   * The sum of the distances of all edges in the given paths.
   */
  public static double distanceMeters(List<StreetPath> paths) {
    return edges(paths).stream().mapToDouble(Edge::getDistanceMeters).sum();
  }

  public static Vertex firstVertex(StreetPath path) {
    return StreetPathStates.states(path).getFirst().getVertex();
  }

  public static Vertex lastVertex(StreetPath path) {
    return StreetPathStates.states(path).getLast().getVertex();
  }

  private static List<Edge> edges(List<StreetPath> paths) {
    return paths
      .stream()
      .flatMap(path -> StreetPathStates.states(path).stream().skip(1))
      .map(State::getBackEdge)
      .toList();
  }
}
