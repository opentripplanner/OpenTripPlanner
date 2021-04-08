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
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;

public class FlexLocationsToStreetEdgesMapper implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(FlexLocationsToStreetEdgesMapper.class);

  @Override
  public void buildGraph(
      Graph graph, HashMap<Class<?>, Object> extra, DataImportIssueStore issueStore
  ) {
    if (graph.locationsById.isEmpty()) {
      return;
    }

    StreetVertexIndex streetIndex = graph.getStreetIndex();

    ProgressTracker progress = ProgressTracker.track("Add flex locations to street vertices", 1, graph.locationsById.size());

    LOG.info(progress.startMessage());
    // TODO: Make this into a parallel stream, first calculate vertices per location and then add them.
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
      // Keep lambda! A method-ref would cause incorrect class and line number to be logged
      progress.step(m -> LOG.info(m));
    }
    LOG.info(progress.completeMessage());
  }

  @Override
  public void checkInputs() {
    // No inputs
  }
}
