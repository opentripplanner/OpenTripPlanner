package org.opentripplanner.ext.flex;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;
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

    var results = timetableRepository
      .getSiteRepository()
      .listAreaStops()
      .parallelStream()
      .sorted(Comparator.comparing(s -> s.getGeometry().getArea()))
      .map(areaStop -> {
        var matchedVertices = matchingVerticesForStop(graph, areaStop);
        // Keep lambda! A method-ref would cause incorrect class and line number to be logged
        progress.step(m -> LOG.info(m));
        return matchedVertices;
      })
      // flapMap converts the stream back to sequential so we use this workaround
      // https://dev.to/hugaomarques/why-your-parallelstream-might-not-be-parallel-at-all-g7e
      .reduce(Stream::concat)
      .orElseGet(Stream::empty);

    ImmutableMultimap<StreetVertex, AreaStop> mappedResults = results.collect(
      ImmutableListMultimap.<MatchResult, StreetVertex, AreaStop>flatteningToImmutableListMultimap(
        MatchResult::vertex,
        mr -> Stream.of(mr.stop())
      )
    );
    results.close();

    mappedResults
      .keySet()
      .parallelStream()
      .forEach(vertex -> vertex.addAreaStops(mappedResults.get(vertex)));

    LOG.info(progress.completeMessage());
  }

  private static Stream<MatchResult> matchingVerticesForStop(Graph graph, AreaStop areaStop) {
    var vertices = graph.findVertices(areaStop.getGeometry().getEnvelopeInternal());

    final var progress = progressTracker(areaStop, vertices);

    var geometryFactory = GeometryUtils.getGeometryFactory();

    return vertices
      .parallelStream()
      .filter(StreetVertex.class::isInstance)
      .map(StreetVertex.class::cast)
      .filter(StreetVertex::isEligibleForCarPickupDropoff)
      .filter(vertx -> {
        // The street index overselects, so need to check for exact geometry inclusion
        Point p = geometryFactory.createPoint(vertx.getCoordinate());
        var intersects = areaStop.getGeometry().intersects(p);
        if (progress != null) {
          // Keep lambda! A method-ref would cause incorrect class and line number to be logged
          progress.step(m -> LOG.info(m));
        }
        return intersects;
      })
      .map(vertx -> new MatchResult(vertx, areaStop));
  }

  /**
   * We want a vertex progress tracker only for large areas, otherwise it becomes too noisy.
   */
  @Nullable
  private static ProgressTracker progressTracker(AreaStop areaStop, Collection<Vertex> vertices) {
    if (vertices.size() < 10_000) {
      return null;
    } else {
      var progress = ProgressTracker.track(
        "Checking vertices of area stop %s".formatted(areaStop.getId()),
        5000,
        vertices.size()
      );
      LOG.info(progress.startMessage());
      return progress;
    }
  }

  /**
   * The result of an area stop being matched with a vertex.
   */
  private record MatchResult(StreetVertex vertex, AreaStop stop) {}
}
