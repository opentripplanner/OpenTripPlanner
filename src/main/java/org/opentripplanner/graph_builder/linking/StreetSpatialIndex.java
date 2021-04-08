package org.opentripplanner.graph_builder.linking;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.routing.graph.Edge;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages street spatial indexes by scope. When linking vertices, visibility is as follows:
 *
 * PERMANENT: Looks at the permanent index and inserts into the permanent index
 * REALTIME: Looks and the permanent index and inserts into the realtime index
 * REQUEST: Looks at both the permanent and realtime indexes and does not insert into any index
 *
 * NOTES ON CONCURRENCY: It is possible that an A Star search would find an edge in the
 * realTimeIndex which is then removed before the actual routing starts. This could result in a
 * NullPointerException on the from/to vertex of the Edge being routed on. This happens seldom
 * enough that we have not accounted for it.
 *
 * A simple way to solve this, if needed, would be to just rerun the search in case on an exception.
 * A more complete solution would have to take into account concurrency not only for the spatial
 * index, but for the entire street graph, as an edge could be removed in the middle of routing.
 *
 * It is also worth noting that the entire reason we have the realTimeIndex in the first place
 * is so that the origin/destination coordinates of a search can connect directly to the edges
 * being split by a realtime update. This is so that we would not have to walk all the way to the
 * end of the edge then back again to where the realtime element was connected.
 * 
 * See #3351
 */
class StreetSpatialIndex {

  private final HashGridSpatialIndex<Edge> permanentIndex = new HashGridSpatialIndex<>();

  private final HashGridSpatialIndex<Edge> realTimeIndex = new HashGridSpatialIndex<>();

  void insert(LineString lineString, Object obj, Scope scope) {
    switch (scope) {
      case PERMANENT:
        permanentIndex.insert(lineString, obj);
        break;
      case REALTIME:
        realTimeIndex.insert(lineString, obj);
        break;
      default:
        throw new IllegalArgumentException();
    }
  }

  void remove(Envelope envelope, final Object item, Scope scope) {
    switch (scope) {
      case PERMANENT:
        permanentIndex.remove(envelope, item);
        return;
      case REALTIME:
        realTimeIndex.remove(envelope, item);
        return;
      default:
        throw new IllegalArgumentException();
    }
  }

  final Stream<Edge> query(Envelope envelope, Scope scope) {
    switch (scope) {
      case PERMANENT:
      case REALTIME:
        return permanentIndex.query(envelope).stream();
      case REQUEST:
        return Stream
            .concat(
                permanentIndex.query(envelope).stream(),
                realTimeIndex.query(envelope).stream()
            );
      default:
        throw new IllegalArgumentException();
    }
  }
}
