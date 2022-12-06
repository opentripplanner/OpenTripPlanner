package org.opentripplanner.graph_builder.module.geometry;

import org.opentripplanner.framework.geometry.WorldEnvelope;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expands envelope to include all stop locations and vertexes in the graph.
 */
public class CalculateWorldEnvelopeModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(CalculateWorldEnvelopeModule.class);

  /**
   * On a fast Mac(2020) 10_000_000 coordinates are processed in a second, so
   * normally spending more tha a second on this will rarely occur. There are
   * usually not that many stops + vertices.
   */
  private static final int LOG_EVERY_N_COORDINATE = 1_000_000;

  private final Graph graph;
  private final TransitModel transitModel;

  public CalculateWorldEnvelopeModule(Graph graph, TransitModel transitModel) {
    this.graph = graph;
    this.transitModel = transitModel;
  }

  @SuppressWarnings("Convert2MethodRef")
  @Override
  public void buildGraph() {
    var vertices = graph.getVertices();
    var stops = transitModel.getStopModel().listStopLocations();

    var progressTracker = ProgressTracker.track(
      "CalculateWorldEnvelope",
      LOG_EVERY_N_COORDINATE,
      vertices.size() + stops.size()
    );
    var e = WorldEnvelope.of();

    for (Vertex v : vertices) {
      e.expandToInclude(v.getCoordinate());
      progressTracker.step(msg -> LOG.info(msg));
    }
    for (var s : stops) {
      WgsCoordinate c = s.getCoordinate();
      e.expandToInclude(c.latitude(), c.longitude());
      progressTracker.step(msg -> LOG.info(msg));
    }
    graph.setEnvelope(e.build());

    LOG.info(progressTracker.completeMessage());
  }

  @Override
  public void checkInputs() {}
}
