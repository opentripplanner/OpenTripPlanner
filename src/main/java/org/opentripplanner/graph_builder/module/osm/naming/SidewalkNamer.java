package org.opentripplanner.graph_builder.module.osm.naming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SidewalkNamer implements EdgeNamer {

  private static final Logger LOG = LoggerFactory.getLogger(SidewalkNamer.class);

  private SpatialIndex streetEdges = new Quadtree();
  private Collection<StreetEdge> unnamedSidewalks = new ArrayList<>();

  @Override
  public I18NString name(OSMWithTags way) {
    return way.getAssumedName();
  }

  @Override
  public void recordEdge(OSMWithTags way, StreetEdge edge) {
    if (way.isSidewalk() && way.needsFallbackName()) {
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
    unnamedSidewalks
      .parallelStream()
      .forEach(sidewalk -> {
        var envelope = sidewalk.getGeometry().getEnvelopeInternal();
        envelope.expandBy(0.0002);
        var candidates = (List<StreetEdge>) streetEdges.query(envelope);

        candidates
          .stream()
          .min(lowestHausdorffDistance(sidewalk))
          .ifPresent(named -> {
            sidewalk.setName(named.getName());
          });

        //Keep lambda! A method-ref would cause incorrect class and line number to be logged
        //noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      });
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
}
