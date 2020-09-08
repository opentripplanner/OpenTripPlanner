package org.opentripplanner.ext.flex;

import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.FlexStopLocation;
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

    for (FlexStopLocation flexStopLocation : graph.locationsById.values()) {
      for (Vertex vertx : streetIndex.getVerticesForEnvelope(flexStopLocation
          .getGeometry()
          .getEnvelopeInternal())
      ) {
        // Check that the vertex is connected to both driveable and walkable edges
        if (!(vertx instanceof StreetVertex)) { continue; }
        if (!((StreetVertex)vertx).isEligibleForCarPickupDropoff()) { continue; }

        // The street index overselects, so need to check for exact geometry inclusion
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
