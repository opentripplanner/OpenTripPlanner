package org.opentripplanner.graph_builder.module.osm.naming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SidewalkNamer implements EdgeNamer {

  private static final Logger LOG = LoggerFactory.getLogger(SidewalkNamer.class);
  private static final int MAX_DISTANCE_TO_SIDEWALK = 50;
  private static final double MIN_PERCENT_IN_BUFFER = .85;

  private HashGridSpatialIndex<EdgeOnLevel> streetEdges = new HashGridSpatialIndex<>();
  private Collection<EdgeOnLevel> unnamedSidewalks = new ArrayList<>();

  @Override
  public I18NString name(OSMWithTags way) {
    return way.getAssumedName();
  }

  @Override
  public void recordEdge(OSMWithTags way, StreetEdge edge) {
    if (way.isSidewalk() && way.needsFallbackName() && !way.isExplicitlyUnnamed()) {
      unnamedSidewalks.add(new EdgeOnLevel(edge, way.getLevels()));
    }
    if (way.isNamed() && !way.isLink()) {
      var containsReverse = streetEdges
        .query(edge.getGeometry().getEnvelopeInternal())
        .stream()
        .anyMatch(candidate -> candidate.edge.isReverseOf(edge));
      if (!containsReverse) {
        streetEdges.insert(
          edge.getGeometry().getEnvelopeInternal(),
          new EdgeOnLevel(edge, way.getLevels())
        );
      }
    }
  }

  @Override
  public void postprocess() {
    ProgressTracker progress = ProgressTracker.track(
      "Assigning names to sidewalks",
      500,
      unnamedSidewalks.size()
    );

    final AtomicInteger namesApplied = new AtomicInteger(0);
    unnamedSidewalks
      .parallelStream()
      .forEach(sidewalkOnLevel -> {
        var sidewalk = sidewalkOnLevel.edge;
        var envelope = sidewalk.getGeometry().getEnvelopeInternal();
        envelope.expandBy(0.000002);
        var candidates = streetEdges.query(envelope);

        var groups = candidates
          .stream()
          .collect(Collectors.groupingBy(e -> e.edge.getName()))
          .entrySet()
          .stream()
          .map(entry -> {
            var levels = entry
              .getValue()
              .stream()
              .flatMap(e -> e.levels.stream())
              .collect(Collectors.toSet());
            return new CandidateGroup(
              entry.getKey(),
              entry.getValue().stream().map(e -> e.edge).toList(),
              levels
            );
          });

        var buffer = preciseBuffer(sidewalk.getGeometry(), 25);
        var sidewalkLength = SphericalDistanceLibrary.length(sidewalk.getGeometry());

        groups
          .filter(g -> g.nearestDistanceTo(sidewalk.getGeometry()) < MAX_DISTANCE_TO_SIDEWALK)
          .filter(g -> g.levels.equals(sidewalkOnLevel.levels))
          .map(g -> {
            var lengthInsideBuffer = g.intersectionLength(buffer);
            double percentInBuffer = lengthInsideBuffer / sidewalkLength;
            return new NamedEdgeGroup(percentInBuffer, candidates.size(), g.name, sidewalk);
          })
          // remove those groups where less than a certain percentage is inside the buffer around
          // the sidewalk. this safety mechanism for sidewalks that snake around the
          // like https://www.openstreetmap.org/way/1059101564
          .filter(group -> group.percentInBuffer > MIN_PERCENT_IN_BUFFER)
          .max(Comparator.comparingDouble(NamedEdgeGroup::percentInBuffer))
          .ifPresent(group -> {
            namesApplied.incrementAndGet();
            sidewalk.setName(Objects.requireNonNull(group.name));
          });

        //Keep lambda! A method-ref would cause incorrect class and line number to be logged
        //noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      });

    LOG.info(
      "Assigned names to {} of {} of sidewalks ({})",
      namesApplied.get(),
      unnamedSidewalks.size(),
      DoubleUtils.roundTo2Decimals((double) namesApplied.get() / unnamedSidewalks.size() * 100)
    );

    LOG.info(progress.completeMessage());

    // set the indices to null so they can be garbage-collected
    streetEdges = null;
    unnamedSidewalks = null;
  }

  /**
   * Taken from https://stackoverflow.com/questions/36455020
   */
  private Geometry preciseBuffer(Geometry geometry, double distanceInMeters) {
    try {
      var coordinate = geometry.getCentroid().getCoordinate();
      String code = "AUTO:42001,%s,%s".formatted(coordinate.x, coordinate.y);
      CoordinateReferenceSystem auto = CRS.decode(code);

      MathTransform toTransform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, auto);
      MathTransform fromTransform = CRS.findMathTransform(auto, DefaultGeographicCRS.WGS84);

      Geometry pGeom = JTS.transform(geometry, toTransform);

      Geometry pBufferedGeom = pGeom.buffer(distanceInMeters, 4, BufferParameters.CAP_FLAT);
      return JTS.transform(pBufferedGeom, fromTransform);
    } catch (TransformException | FactoryException e) {
      throw new RuntimeException(e);
    }
  }

  record NamedEdgeGroup(
    double percentInBuffer,
    int numberOfCandidates,
    I18NString name,
    StreetEdge sidewalk
  ) {
    NamedEdgeGroup {
      Objects.requireNonNull(name);
      Objects.requireNonNull(sidewalk);
    }
  }

  record CandidateGroup(I18NString name, List<StreetEdge> edges, Set<String> levels) {
    double intersectionLength(Geometry polygon) {
      return edges
        .stream()
        .mapToDouble(edge -> {
          var intersection = polygon.intersection(edge.getGeometry());
          return length(intersection);
        })
        .sum();
    }

    private double length(Geometry intersection) {
      return switch (intersection) {
        case LineString ls -> SphericalDistanceLibrary.length(ls);
        case MultiLineString mls -> GeometryUtils
          .getLineStrings(mls)
          .stream()
          .mapToDouble(this::intersectionLength)
          .sum();
        case Point ignored -> 0;
        case Geometry g -> throw new IllegalStateException(
          "Didn't expect geometry %s".formatted(g.getClass())
        );
      };
    }

    double nearestDistanceTo(Geometry g) {
      return edges
        .stream()
        .mapToDouble(e -> {
          var points = DistanceOp.nearestPoints(e.getGeometry(), g);
          return SphericalDistanceLibrary.fastDistance(points[0], points[1]);
        })
        .min()
        .orElse(Double.MAX_VALUE);
    }
  }

  record EdgeOnLevel(StreetEdge edge, Set<String> levels) {}
}
