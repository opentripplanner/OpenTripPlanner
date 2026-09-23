package org.opentripplanner.street.linking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * Collects the street edges within the search radius of a vertex from the spatial-index
 * candidates. Only street edges traversable by at least one of the given modes, within the search
 * radius and still present in the graph are kept. The index may report the same edge once per grid
 * cell it spans, so survivors are deduplicated with the same {@code equals}/{@code hashCode}
 * semantics the set-based index query used to apply to every candidate.
 */
final class NearbyStreetEdgeCollector implements Consumer<Edge> {

  private final double lon;
  private final double lat;
  private final double xscale;
  /**
   * Squared search radius, compared with
   * {@link StreetEdge#squaredEquirectangularDistanceToPoint} values.
   */
  private final double radiusDegSq;
  private final TraverseModeSet traverseModes;
  private final Set<StreetEdge> seen = new HashSet<>();
  private final List<DistanceTo> nearbyEdges = new ArrayList<>();

  NearbyStreetEdgeCollector(
    Vertex vertex,
    TraverseModeSet traverseModes,
    double radiusDeg,
    double xscale
  ) {
    this.lon = vertex.getLon();
    this.lat = vertex.getLat();
    this.xscale = xscale;
    this.radiusDegSq = radiusDeg * radiusDeg;
    this.traverseModes = traverseModes;
  }

  @Override
  public void accept(Edge edge) {
    // The predicates are conjunctive, so the order only affects speed, not the result. The mode
    // check is a single bitmask test and is by far the most selective filter for modes that only
    // a fraction of the edges allow (for car it rejects around three quarters of the candidates),
    // so it runs before the distance computation.
    if (!(edge instanceof StreetEdge streetEdge) || !streetEdge.canTraverse(traverseModes)) {
      return;
    }
    double squaredDistance = streetEdge.squaredEquirectangularDistanceToPoint(lon, lat, xscale);
    if (squaredDistance >= radiusDegSq || !seen.add(streetEdge)) {
      return;
    }
    if (streetEdge.isReachableFromGraph()) {
      nearbyEdges.add(new DistanceTo(streetEdge, squaredDistance));
    }
  }

  List<DistanceTo> nearbyEdges() {
    return nearbyEdges;
  }
}
