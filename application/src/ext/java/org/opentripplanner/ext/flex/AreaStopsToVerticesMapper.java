package org.opentripplanner.ext.flex;

import jakarta.inject.Inject;
import java.util.ArrayList;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.vertex.StreetVertex;
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

    var geometryFactory = GeometryUtils.getGeometryFactory();
    var vertices = graph.getVertices();

    var areaStops = timetableRepository.getSiteRepository().listAreaStops();
    var envs = areaStops.stream().map(s -> s.getGeometry().getEnvelopeInternal()).toList();

    var progress = ProgressTracker.track("Adding area stops to vertices", 50_000, vertices.size());

    vertices
      .parallelStream()
      .filter(StreetVertex.class::isInstance)
      .map(StreetVertex.class::cast)
      .filter(StreetVertex::isEligibleForCarPickupDropoff)
      // a very fast check to exclude vertices that are far away from any area stop
      .filter(s -> envs.stream().anyMatch(env -> env.contains(s.getCoordinate())))
      .forEach(vertx -> {
        // slow, precise check if the vertex is within an area stop
        Point p = geometryFactory.createPoint(vertx.getCoordinate());
        var toBeAdded = new ArrayList<AreaStop>();
        for (var areaStop : areaStops) {
          if (areaStop.getGeometry().intersects(p)) {
            toBeAdded.add(areaStop);
          }
        }
        vertx.addAreaStops(toBeAdded);

        progress.step(m -> LOG.info(m));
      });
    LOG.info(progress.completeMessage());
  }
}
