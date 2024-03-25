package org.opentripplanner.graph_builder.module.osm.naming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SidewalkNamer implements EdgeNamer {

  private static final Logger LOG = LoggerFactory.getLogger(SidewalkNamer.class);
  private static final int MAX_DISTANCE_TO_SIDEWALK = 50;

  private SpatialIndex streetEdges = new HashGridSpatialIndex<StreetEdge>();
  private Collection<StreetEdge> unnamedSidewalks = new ArrayList<>();

  @Override
  public I18NString name(OSMWithTags way) {
    return way.getAssumedName();
  }

  @Override
  public void recordEdge(OSMWithTags way, StreetEdge edge) {
    if (way.isSidewalk() && way.needsFallbackName() && !way.isExplicitlyUnnamed()) {
      unnamedSidewalks.add(edge);
    }
    if (way.isNamed()) {
      streetEdges.insert(edge.getGeometry().getEnvelopeInternal(), edge);
    }
  }

  @Override
  public void postprocess() {
    ProgressTracker progress = ProgressTracker.track(
      "Assigning names to sidewalks",
      500,
      unnamedSidewalks.size()
    );

    List<EdgeWithDistance> edges = new ArrayList<>();
    unnamedSidewalks
      .parallelStream()
      .forEach(sidewalk -> {
        var envelope = sidewalk.getGeometry().getEnvelopeInternal();
        envelope.expandBy(0.000002);
        var candidates = (List<StreetEdge>) streetEdges.query(envelope);

        candidates
          .stream()
          .map(c -> {
            var hausdorff = DiscreteHausdorffDistance.distance(
              sidewalk.getGeometry(),
              c.getGeometry(),0.5
            );

            var points = DistanceOp.nearestPoints( c.getGeometry(),sidewalk.getGeometry());
            double fastDistance = SphericalDistanceLibrary.fastDistance(points[0], points[1]);

            return new EdgeWithDistance(hausdorff, fastDistance, candidates.size(), c, sidewalk);
          })
          .filter(e -> e.distance < MAX_DISTANCE_TO_SIDEWALK)
          .min(Comparator.comparingDouble(EdgeWithDistance::hausdorff))
          .ifPresent(named -> {
            edges.add(named);
            sidewalk.setName(Objects.requireNonNull(named.namedEdge.getName()));
          });

        //Keep lambda! A method-ref would cause incorrect class and line number to be logged
        //noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      });

    edges
      .stream()
      .sorted(Comparator.comparingDouble(EdgeWithDistance::hausdorff).reversed())
      .limit(100)
      .forEach(EdgeWithDistance::logDebugString);
    LOG.info(progress.completeMessage());

    // set the indices to null so they can be garbage-collected
    streetEdges = null;
    unnamedSidewalks = null;
  }

  private static Comparator<StreetEdge> lowestHausdorffDistance(StreetEdge sidewalk) {
    return Comparator.comparingDouble(candidate ->
      DiscreteHausdorffDistance.distance(sidewalk.getGeometry(), candidate.getGeometry())
    );
  }

  record EdgeWithDistance(
    double hausdorff,
    double distance,
    int numberOfCandidates,
    StreetEdge namedEdge,
    StreetEdge sidewalk
  ) {
    void logDebugString() {
      LOG.info("Name '{}' applied with low Hausdorff distance ", namedEdge.getName());
      LOG.info("Hausdorff:     {}", hausdorff);
      LOG.info("Distance:      {}m", distance);
      LOG.info("OSM:           {}", osmUrl());
      LOG.info("Debug client:  {}", debugClientUrl());
      LOG.info("<-------------------------------------------------------------------------------->");
    }

    String debugClientUrl() {
      var c = new WgsCoordinate(sidewalk.getFromVertex().getCoordinate());
      return "http://localhost:8080/debug-client-preview/#19/%s/%s".formatted(
          c.latitude(),
          c.longitude()
        );
    }

    String osmUrl() {
      var c = new WgsCoordinate(sidewalk.getFromVertex().getCoordinate());
      return "https://www.openstreetmap.org/?mlat=%s&mlon=%s#map=17/%s/%s".formatted(
          c.latitude(),
          c.longitude(),
          c.latitude(),
          c.longitude()
        );
    }
  }
}
