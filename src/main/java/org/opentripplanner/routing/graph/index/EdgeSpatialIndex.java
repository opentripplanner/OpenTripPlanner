package org.opentripplanner.routing.graph.index;

import java.util.stream.Stream;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.routing.linking.Scope;
import org.opentripplanner.street.model.edge.Edge;

/**
 * Manages edge spatial indexes by scope. When linking vertices, visibility is as follows:
 * <p>
 * PERMANENT: Looks at the permanent index and inserts into the permanent index REALTIME: Looks and
 * the permanent index and inserts into the realtime index REQUEST: Looks at both the permanent and
 * realtime indexes and does not insert into any index
 * <p>
 * NOTES ON CONCURRENCY: It is possible that an A Star search would find an edge in the
 * realTimeIndex which is then removed before the actual routing starts. This could result in a
 * NullPointerException on the from/to vertex of the Edge being routed on. This happens seldom
 * enough that we have not accounted for it.
 * <p>
 * A simple way to solve this, if needed, would be to just rerun the search in case on an exception.
 * A more complete solution would have to take into account concurrency not only for the spatial
 * index, but for the entire street graph, as an edge could be removed in the middle of routing.
 * <p>
 * It is also worth noting that the entire reason we have the realTimeIndex in the first place is so
 * that the origin/destination coordinates of a search can connect directly to the edges being split
 * by a realtime update. This is so that we would not have to walk all the way to the end of the
 * edge then back again to where the realtime element was connected.
 * <p>
 * See #3351
 */
public class EdgeSpatialIndex {

  private final HashGridSpatialIndex<Edge> permanentEdgeIndex = new HashGridSpatialIndex<>();

  private final HashGridSpatialIndex<Edge> realTimeEdgeIndex = new HashGridSpatialIndex<>();

  public void insert(LineString lineString, Object obj, Scope scope) {
    switch (scope) {
      case PERMANENT -> permanentEdgeIndex.insert(lineString, obj);
      case REALTIME -> realTimeEdgeIndex.insert(lineString, obj);
      case REQUEST -> throw new IllegalArgumentException();
    }
  }

  public void remove(Envelope envelope, final Object item, Scope scope) {
    switch (scope) {
      case PERMANENT -> permanentEdgeIndex.remove(envelope, item);
      case REALTIME -> realTimeEdgeIndex.remove(envelope, item);
      case REQUEST -> throw new IllegalArgumentException();
    }
  }

  public final Stream<Edge> query(Envelope envelope, Scope scope) {
    return switch (scope) {
      case PERMANENT, REALTIME -> permanentEdgeIndex.query(envelope).stream();
      case REQUEST -> Stream.concat(
        permanentEdgeIndex.query(envelope).stream(),
        realTimeEdgeIndex.query(envelope).stream()
      );
    };
  }

  public void compact() {
    permanentEdgeIndex.compact();
  }
}
