package org.opentripplanner.graph_builder.module;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildService;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;

/**
 * Which platform a street edge or area belongs to, during boarding location linking.
 * <p>
 * The platforms OSM processing found are keyed by edge reference, an association that is lost when
 * linking splits a platform edge in two. The halves are recorded here rather than back in
 * {@code OsmInfoGraphBuildRepository}: nothing after this module reads them, and a linking module
 * has no business writing to a repository another module owns.
 */
class PlatformLookup {

  private final OsmInfoGraphBuildService osmInfo;
  private final Map<Edge, Platform> platformsOfSplitEdges = new HashMap<>();

  PlatformLookup(OsmInfoGraphBuildService osmInfo) {
    this.osmInfo = osmInfo;
  }

  Optional<Platform> findPlatform(Edge edge) {
    return osmInfo
      .findPlatform(edge)
      .or(() -> Optional.ofNullable(platformsOfSplitEdges.get(edge)));
  }

  Optional<Platform> findPlatform(Area area) {
    return osmInfo.findPlatform(area);
  }

  /**
   * Record the halves that splitting one of {@code platform}'s edges at {@code vertex} produced, so
   * a later stop on the same platform still finds it. Only a genuine split produces a
   * {@link SplitterVertex}: if the boarding location snapped to an existing endpoint, the original
   * edge is still registered and that endpoint's other incident edges must not be recorded, or a
   * later stop could match a platform it is not on.
   */
  void addSplitEdges(StreetVertex vertex, Platform platform) {
    if (!(vertex instanceof SplitterVertex)) {
      return;
    }
    Stream.concat(vertex.getIncoming().stream(), vertex.getOutgoing().stream())
      .filter(StreetEdge.class::isInstance)
      .forEach(edge -> platformsOfSplitEdges.put(edge, platform));
  }
}
