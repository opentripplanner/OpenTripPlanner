package org.opentripplanner.ext.flex;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import jakarta.inject.Inject;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterates over all area stops in the stop model and adds them to vertices that are suitable for
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
    if (!timetableRepository.getStopModel().hasAreaStops()) {
      return;
    }

    StreetIndex streetIndex = graph.getStreetIndexSafe(timetableRepository.getStopModel());

    ProgressTracker progress = ProgressTracker.track(
      "Add flex locations to street vertices",
      1,
      timetableRepository.getStopModel().listAreaStops().size()
    );

    LOG.info(progress.startMessage());
    var results = timetableRepository
      .getStopModel()
      .listAreaStops()
      .parallelStream()
      .flatMap(areaStop -> {
        var matchedVertices = matchingVerticesForStop(streetIndex, areaStop);
        // Keep lambda! A method-ref would cause incorrect class and line number to be logged
        progress.step(m -> LOG.info(m));
        return matchedVertices;
      });

    ImmutableMultimap<StreetVertex, AreaStop> mappedResults = results.collect(
      ImmutableListMultimap.<MatchResult, StreetVertex, AreaStop>flatteningToImmutableListMultimap(
        MatchResult::vertex,
        mr -> Stream.of(mr.stop())
      )
    );

    mappedResults
      .keySet()
      .forEach(vertex -> {
        vertex.addAreaStops(mappedResults.get(vertex));
      });

    LOG.info(progress.completeMessage());
  }

  private static Stream<MatchResult> matchingVerticesForStop(
    StreetIndex streetIndex,
    AreaStop areaStop
  ) {
    return streetIndex
      .getVerticesForEnvelope(areaStop.getGeometry().getEnvelopeInternal())
      .stream()
      .filter(StreetVertex.class::isInstance)
      .map(StreetVertex.class::cast)
      .filter(StreetVertex::isEligibleForCarPickupDropoff)
      .filter(vertx -> {
        // The street index overselects, so need to check for exact geometry inclusion
        Point p = GeometryUtils.getGeometryFactory().createPoint(vertx.getCoordinate());
        return areaStop.getGeometry().intersects(p);
      })
      .map(vertx -> new MatchResult(vertx, areaStop));
  }

  /**
   * The result of an area stop being matched with a vertex.
   */
  private record MatchResult(StreetVertex vertex, AreaStop stop) {}
}
