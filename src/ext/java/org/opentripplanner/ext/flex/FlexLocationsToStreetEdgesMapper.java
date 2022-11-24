package org.opentripplanner.ext.flex;

import java.util.HashSet;
import javax.inject.Inject;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlexLocationsToStreetEdgesMapper implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(FlexLocationsToStreetEdgesMapper.class);

  private final Graph graph;
  private final TransitModel transitModel;

  @Inject
  public FlexLocationsToStreetEdgesMapper(Graph graph, TransitModel transitModel) {
    this.graph = graph;
    this.transitModel = transitModel;
  }

  @Override
  @SuppressWarnings("Convert2MethodRef")
  public void buildGraph() {
    if (!transitModel.getStopModel().hasAreaStops()) {
      return;
    }

    StreetIndex streetIndex = graph.getStreetIndexSafe(transitModel.getStopModel());

    ProgressTracker progress = ProgressTracker.track(
      "Add flex locations to street vertices",
      1,
      transitModel.getStopModel().listAreaStops().size()
    );

    LOG.info(progress.startMessage());
    // TODO: Make this into a parallel stream, first calculate vertices per location and then add them.
    for (AreaStop areaStop : transitModel.getStopModel().listAreaStops()) {
      for (Vertex vertx : streetIndex.getVerticesForEnvelope(
        areaStop.getGeometry().getEnvelopeInternal()
      )) {
        // Check that the vertex is connected to both driveable and walkable edges
        if (!(vertx instanceof StreetVertex streetVertex)) {
          continue;
        }
        if (!((StreetVertex) vertx).isEligibleForCarPickupDropoff()) {
          continue;
        }

        // The street index overselects, so need to check for exact geometry inclusion
        Point p = GeometryUtils.getGeometryFactory().createPoint(vertx.getCoordinate());
        if (areaStop.getGeometry().disjoint(p)) {
          continue;
        }

        if (streetVertex.areaStops == null) {
          streetVertex.areaStops = new HashSet<>();
        }

        streetVertex.areaStops.add(areaStop);
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
