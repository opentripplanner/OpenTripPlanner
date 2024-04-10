package org.opentripplanner.graph_builder.module.osm.naming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

/**
 * A namer that assigns names of nearby streets to sidewalks if they meet certain
 * geometric similarity criteria.
 * <p>
 * The algorithm works as follows:
 *  - for each sidewalk we look up (named) street edges nearby
 *  - group those edges into groups where each edge has the same name
 *  - draw a flat-capped buffer around the sidewalk, like this: https://tinyurl.com/4fpe882h
 *  - check how much of a named edge group is inside the buffer
 *  - remove those groups which are below MIN_PERCENT_IN_BUFFER
 *  - take the group that has the highest percentage (as a proportion of the sidewalk length) inside
 *    the buffer and apply its name to the sidewalk.
 * <p>
 * This works very well for OSM data where the sidewalk runs a parallel to the street and at each
 * intersection the sidewalk is also split. It doesn't work well for sidewalks that go around
 * the corner, like https://www.openstreetmap.org/way/1059101564. These cases are, however, detected
 * by the above algorithm and the sidewalk name remains the same.
 */
public class SidewalkNamer implements EdgeNamer {

  private static final Logger LOG = LoggerFactory.getLogger(SidewalkNamer.class);
  private static final int MAX_DISTANCE_TO_SIDEWALK = 50;
  private static final double MIN_PERCENT_IN_BUFFER = .85;
  private static final int BUFFER_METERS = 25;

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
      // we generate two edges for each osm way: one there and one back. since we don't do any routing
      // in this class we don't need the two directions and keep only one of them.
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
        assignNameToSidewalk(sidewalkOnLevel, namesApplied);

        // Keep lambda! A method-ref would cause incorrect class and line number to be logged
        // noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      });

    LOG.info(
      "Assigned names to {} of {} of sidewalks ({}%)",
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
   * The actual worker method that runs the business logic on an individual sidewalk edge.
   */
  private void assignNameToSidewalk(EdgeOnLevel sidewalkOnLevel, AtomicInteger namesApplied) {
    var sidewalk = sidewalkOnLevel.edge;
    var buffer = preciseBuffer(sidewalk.getGeometry(), BUFFER_METERS);
    var sidewalkLength = SphericalDistanceLibrary.length(sidewalk.getGeometry());

    var envelope = sidewalk.getGeometry().getEnvelopeInternal();
    envelope.expandBy(0.000002);
    var candidates = streetEdges.query(envelope);

    groupEdgesByName(candidates)
      // remove edges that are far away
      .filter(g -> g.nearestDistanceTo(sidewalk.getGeometry()) < MAX_DISTANCE_TO_SIDEWALK)
      // make sure we only compare sidewalks and streets that are on the same level
      .filter(g -> g.levels.equals(sidewalkOnLevel.levels))
      .map(g -> computePercentInsideBuffer(g, buffer, sidewalkLength))
      // Remove those groups where less than a certain percentage is inside the buffer around
      // the sidewalk. This is a safety mechanism for sidewalks that snake around the corner,
      // like https://www.openstreetmap.org/way/1059101564 .
      .filter(group -> group.percentInBuffer > MIN_PERCENT_IN_BUFFER)
      .max(Comparator.comparingDouble(NamedEdgeGroup::percentInBuffer))
      .ifPresent(group -> {
        namesApplied.incrementAndGet();
        sidewalk.setName(Objects.requireNonNull(group.name));
      });
  }

  /**
   * Compute the length of the group that is inside the buffer and return it as a percentage
   * of the length of the sidewalk.
   */
  private static NamedEdgeGroup computePercentInsideBuffer(
    CandidateGroup g,
    Geometry buffer,
    double sidewalkLength
  ) {
    var lengthInsideBuffer = g.intersectionLength(buffer);
    double percentInBuffer = lengthInsideBuffer / sidewalkLength;
    return new NamedEdgeGroup(percentInBuffer, g.name);
  }

  /**
   * If a single street is split into several edges, each individual part of the street would potentially
   * have a low similarity with the (longer) sidewalk. For that reason we combine them into a group
   * and have a better basis for comparison.
   */
  private static Stream<CandidateGroup> groupEdgesByName(List<EdgeOnLevel> candidates) {
    return candidates
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
  }

  /**
   * Add a buffer around a geometry that makes sure that the buffer is the same distance (in meters)
   * anywhere on earth.
   * <p>
   * Background: If you call the regular buffer() method on a JTS geometry that uses WGS84 as the
   * coordinate reference system, the buffer will be accurate at the equator but will become more
   * and more elongated the further north/south you go.
   * <p>
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

  private record NamedEdgeGroup(double percentInBuffer, I18NString name) {
    NamedEdgeGroup {
      Objects.requireNonNull(name);
    }
  }

  /**
   * A group of edges that are near a sidewalk that have the same name. These groups are used
   * to figure out if the name of the group can be applied to a nearby sidewalk.
   */
  private record CandidateGroup(I18NString name, List<StreetEdge> edges, Set<String> levels) {
    /**
     * How much of this group intersects with the given geometry, in meters.
     */
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

    /**
     * Get the closest distance in meters between any of the edges in the group and the given geometry.
     */
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

  private record EdgeOnLevel(StreetEdge edge, Set<String> levels) {}
}
