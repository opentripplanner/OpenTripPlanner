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
 * PERMANENT: Can only see the permanent index
 * REALTIME: Can only see the permanent index
 * REQUEST: Can see both the permanent and realtime indexes
 *
 * A request-scoped index is not needed, as we do not want request-scoped edges to be visible
 * when linking.
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

   boolean remove(Envelope envelope, final Object item, Scope scope) {
    switch (scope) {
      case PERMANENT:
        return permanentIndex.remove(envelope, item);
      case REALTIME:
        return realTimeIndex.remove(envelope, item);
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
