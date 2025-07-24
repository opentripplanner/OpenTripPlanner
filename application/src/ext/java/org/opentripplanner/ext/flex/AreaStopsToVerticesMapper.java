package org.opentripplanner.ext.flex;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterates over all area stops in the stop and adds them to vertices that are suitable for
 * boarding flex trips.
 */
public class AreaStopsToVerticesMapper implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(AreaStopsToVerticesMapper.class);
  /**
   * The number of vertices that above which an area is considered "large" and therefore will
   * have a progress tracker about processing the geometry.
   */
  private static final int LARGE_AREA_LIMIT = 30_000;

  private final Graph graph;
  private final TimetableRepository timetableRepository;

  @Inject
  public AreaStopsToVerticesMapper(Graph graph, TimetableRepository timetableRepository) {
    this.graph = graph;
    this.timetableRepository = timetableRepository;
  }

  @Override
  @SuppressWarnings("Convert2MethodRef")
  public void buildGraph() {
    if (!timetableRepository.getSiteRepository().hasAreaStops()) {
      return;
    }

    ProgressTracker progress = ProgressTracker.track(
      "Add flex locations to street vertices",
      1,
      timetableRepository.getSiteRepository().listAreaStops().size()
    );

    LOG.info(progress.startMessage());

    timetableRepository
      .getSiteRepository()
      .listAreaStops()
      .parallelStream()
      .forEach(areaStop -> {
        applyAreaStopToVertices(areaStop, graph);
        // Keep lambda! A method-ref would cause incorrect class and line number to be logged
        progress.step(m -> LOG.info(m));
      });

    LOG.info(progress.completeMessage());
  }

  private static void applyAreaStopToVertices(AreaStop areaStop, Graph graph) {
    var vertices = graph.findVertices(areaStop.getGeometry().getEnvelopeInternal());

    @Nullable
    var progress = progressTracker(areaStop, vertices);

    var geometryFactory = GeometryUtils.getGeometryFactory();

    vertices
      .parallelStream()
      .filter(StreetVertex.class::isInstance)
      .map(StreetVertex.class::cast)
      .filter(StreetVertex::isEligibleForCarPickupDropoff)
      .forEach(vertx -> {
        // The street index overselects, so need to check for exact geometry inclusion
        Point p = geometryFactory.createPoint(vertx.getCoordinate());
        var intersects = areaStop.getGeometry().intersects(p);
        if (progress != null) {
          // Keep lambda! A method-ref would cause incorrect class and line number to be logged
          progress.step(m -> LOG.info(m));
        }
        if (intersects) {
          vertx.addAreaStops(List.of(areaStop));
        }
      });
    if (progress != null) {
      LOG.info(progress.completeMessage());
    }
  }

  /**
   * We want a vertex progress tracker only for large areas, otherwise it becomes too noisy.
   */
  @Nullable
  private static ProgressTracker progressTracker(AreaStop areaStop, Collection<Vertex> vertices) {
    if (vertices.size() < LARGE_AREA_LIMIT) {
      return null;
    } else {
      var progress = ProgressTracker.track(
        "Computing vertices of area stop %s".formatted(areaStop.getId()),
        10000,
        vertices.size()
      );
      LOG.info(progress.startMessage());
      return progress;
    }
  }
}
