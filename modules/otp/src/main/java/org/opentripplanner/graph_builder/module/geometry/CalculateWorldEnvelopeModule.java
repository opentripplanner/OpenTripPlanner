package org.opentripplanner.graph_builder.module.geometry;

import jakarta.inject.Inject;
import java.util.Collection;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.StopLocation;
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
  private final WorldEnvelopeRepository worldEnvelopeRepository;

  @Inject
  public CalculateWorldEnvelopeModule(
    Graph graph,
    TransitModel transitModel,
    WorldEnvelopeRepository worldEnvelopeRepository
  ) {
    this.graph = graph;
    this.transitModel = transitModel;
    this.worldEnvelopeRepository = worldEnvelopeRepository;
  }

  @Override
  public void buildGraph() {
    var vertices = graph.getVertices();
    var stops = transitModel.getStopModel().listStopLocations();
    WorldEnvelope envelope = build(vertices, stops);
    worldEnvelopeRepository.saveEnvelope(envelope);
  }

  @SuppressWarnings("Convert2MethodRef")
  static WorldEnvelope build(
    Collection<? extends Vertex> vertices,
    Collection<? extends StopLocation> stops
  ) {
    var progressTracker = ProgressTracker.track(
      "CalculateWorldEnvelope",
      LOG_EVERY_N_COORDINATE,
      vertices.size() + stops.size()
    );
    var e = WorldEnvelope.of();

    for (Vertex v : vertices) {
      var c = v.getCoordinate();
      e.expandToIncludeStreetEntities(c.y, c.x);
      progressTracker.step(msg -> LOG.info(msg));
    }

    // We need to iterate over the stops several times to compute bounding-box
    // and median center; Hence the awkward method call.
    e.expandToIncludeTransitEntities(
      stops,
      s -> s.getCoordinate().latitude(),
      s -> s.getCoordinate().longitude()
    );
    progressTracker.steps(stops.size(), msg -> LOG.info(msg));

    LOG.info(progressTracker.completeMessage());

    return e.build();
  }
}
