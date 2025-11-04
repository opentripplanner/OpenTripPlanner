package org.opentripplanner.graph_builder.module.osm.naming;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;

/**
 * Base class for namers that use a geo buffer to query geo features.
 */
class BufferedEdgeProcessor {

  private final String type;
  private final Logger logger;
  private final int bufferMeters;
  private final AssignNameToEdge assigner;

  BufferedEdgeProcessor(int bufferMeters, String type, Logger logger, AssignNameToEdge assigner) {
    this.type = type;
    this.logger = logger;
    this.bufferMeters = bufferMeters;
    this.assigner = assigner;
  }

  public void applyNames(Collection<EdgeOnLevel> unnamedEdges) {
    ProgressTracker progress = ProgressTracker.track(
      String.format("Assigning names to %s", type),
      500,
      unnamedEdges.size()
    );
    PreciseBufferFactory preciseBufferFactory = new PreciseBufferFactory(
      computeEnvelopeCenter(unnamedEdges),
      bufferMeters
    );

    final AtomicInteger namesApplied = new AtomicInteger(0);
    unnamedEdges
      .parallelStream()
      .forEach(edgeOnLevel -> {
        var buffer = preciseBufferFactory.preciseBuffer(edgeOnLevel.edge().getGeometry());
        if (assigner.assignNameToEdge(edgeOnLevel, buffer)) {
          namesApplied.incrementAndGet();
        }

        // Keep lambda! A method-ref would cause incorrect class and line number to be logged
        // noinspection Convert2MethodRef
        progress.step(m -> logger.info(m));
      });

    logger.info(
      "Assigned names to {} of {} {} ({}%)",
      namesApplied.get(),
      unnamedEdges.size(),
      type,
      DoubleUtils.roundTo2Decimals(((double) namesApplied.get() / unnamedEdges.size()) * 100)
    );

    logger.info(progress.completeMessage());
  }

  /**
   * Compute the centroid of all sidewalk edges.
   */
  private static Coordinate computeEnvelopeCenter(Collection<EdgeOnLevel> edges) {
    var envelope = new Envelope();
    edges.forEach(e -> {
      envelope.expandToInclude(e.edge().getFromVertex().getCoordinate());
      envelope.expandToInclude(e.edge().getToVertex().getCoordinate());
    });
    return envelope.centre();
  }
}
