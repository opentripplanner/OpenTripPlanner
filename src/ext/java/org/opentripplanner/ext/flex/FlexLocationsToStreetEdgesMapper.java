package org.opentripplanner.ext.flex;

import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.util.HashMap;
import java.util.HashSet;

public class FlexLocationsToStreetEdgesMapper implements GraphBuilderModule {

  @Override
  public void buildGraph(
      Graph graph, HashMap<Class<?>, Object> extra, DataImportIssueStore issueStore
  ) {
    if (graph.locationsById.isEmpty()) {
      return;
    }

    StreetVertexIndex streetIndex = new StreetVertexIndex(graph);
    TraverseModeSet carTraverseModeSet = new TraverseModeSet(TraverseMode.CAR);
    TraverseModeSet walkTraverseModeSet = new TraverseModeSet(TraverseMode.WALK);

    for (FlexStopLocation flexStopLocation : graph.locationsById.values()) {
      for (Vertex vertx : streetIndex.getVerticesForEnvelope(flexStopLocation
          .getGeometry()
          .getEnvelopeInternal())
      ) {
        if (!(vertx instanceof StreetVertex)) { continue; }
        if (vertx.getOutgoing().stream().noneMatch(edge ->
            edge instanceof StreetEdge && ((StreetEdge) edge).canTraverse(carTraverseModeSet))  ||
            vertx.getOutgoing().stream().noneMatch(edge ->
            edge instanceof StreetEdge && ((StreetEdge) edge).canTraverse(walkTraverseModeSet))
        ) {
          continue;
        }

        Point p = GeometryUtils.getGeometryFactory().createPoint(vertx.getCoordinate());
        if (flexStopLocation.getGeometry().disjoint(p)) {
          continue;
        }

        StreetVertex streetVertex = (StreetVertex) vertx;

        if (streetVertex.flexStopLocations == null) {
          streetVertex.flexStopLocations = new HashSet<>();
        }

        streetVertex.flexStopLocations.add(flexStopLocation);
      }
    }
  }

  @Override
  public void checkInputs() {
    // No inputs
  }
}
