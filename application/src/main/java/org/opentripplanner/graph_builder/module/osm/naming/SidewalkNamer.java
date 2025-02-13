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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.logging.ProgressTracker;
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
  private static final double MIN_PERCENT_IN_BUFFER = .85;
  private static final int BUFFER_METERS = 25;

  private HashGridSpatialIndex<EdgeOnLevel> streetEdges = new HashGridSpatialIndex<>();
  private Collection<EdgeOnLevel> unnamedSidewalks = new ArrayList<>();
  private PreciseBuffer preciseBuffer;

  @Override
  public I18NString name(OsmEntity way) {
    return way.getAssumedName();
  }

  @Override
  public void recordEdges(OsmEntity way, StreetEdgePair pair) {
    // This way is a sidewalk and hasn't been named yet (and is not explicitly unnamed)
    if (way.isSidewalk() && way.hasNoName() && !way.isExplicitlyUnnamed()) {
      pair
        .asIterable()
        .forEach(edge -> unnamedSidewalks.add(new EdgeOnLevel(edge, way.getLevels())));
    }
    // The way is _not_ a sidewalk and does have a name
    else if (way.isNamed() && !way.isLink()) {
      // We generate two edges for each osm way: one there and one back. This spatial index only
      // needs to contain one item for each road segment with a unique geometry and name, so we
      // add only one of the two edges.
      var edge = pair.pickAny();
      streetEdges.insert(
        edge.getGeometry().getEnvelopeInternal(),
        new EdgeOnLevel(edge, way.getLevels())
      );
    }
  }

  @Override
  public void postprocess() {
    ProgressTracker progress = ProgressTracker.track(
      "Assigning names to sidewalks",
      500,
      unnamedSidewalks.size()
    );

    this.preciseBuffer = new PreciseBuffer(computeEnvelopeCenter(), BUFFER_METERS);

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

    // Set the indices to null so they can be garbage-collected
    streetEdges = null;
    unnamedSidewalks = null;
  }

  /**
   * Compute the centroid of all sidewalk edges.
   */
  private Coordinate computeEnvelopeCenter() {
    var envelope = new Envelope();
    unnamedSidewalks.forEach(e -> {
      envelope.expandToInclude(e.edge.getFromVertex().getCoordinate());
      envelope.expandToInclude(e.edge.getToVertex().getCoordinate());
    });
    return envelope.centre();
  }

  /**
   * The actual worker method that runs the business logic on an individual sidewalk edge.
   */
  private void assignNameToSidewalk(EdgeOnLevel sidewalkOnLevel, AtomicInteger namesApplied) {
    var sidewalk = sidewalkOnLevel.edge;
    var buffer = preciseBuffer.preciseBuffer(sidewalk.getGeometry());
    var sidewalkLength = SphericalDistanceLibrary.length(sidewalk.getGeometry());

    var candidates = streetEdges.query(buffer.getEnvelopeInternal());

    groupEdgesByName(candidates)
      // Make sure we only compare sidewalks and streets that are on the same level
      .filter(g -> g.levels.equals(sidewalkOnLevel.levels))
      .map(g -> computePercentInsideBuffer(g, buffer, sidewalkLength))
      // Remove those groups where less than a certain percentage is inside the buffer around
      // the sidewalk. This is a safety mechanism for sidewalks that snake around the corner,
      // like https://www.openstreetmap.org/way/1059101564.
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
  }

  private record EdgeOnLevel(StreetEdge edge, Set<String> levels) {}

  /**
   * A class to cache the expensive construction of a Universal Traverse Mercator coordinate
   * reference system.
   * Re-using the same CRS for all edges might introduce tiny imprecisions for OTPs use cases
   * but speeds up the processing enormously and is a price well worth paying.
   */
  private static final class PreciseBuffer {

    private final double distanceInMeters;
    private final MathTransform toTransform;
    private final MathTransform fromTransform;

    private PreciseBuffer(Coordinate coordinate, double distanceInMeters) {
      this.distanceInMeters = distanceInMeters;
      String code = "AUTO:42001,%s,%s".formatted(coordinate.x, coordinate.y);
      try {
        CoordinateReferenceSystem auto = CRS.decode(code);
        this.toTransform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, auto);
        this.fromTransform = CRS.findMathTransform(auto, DefaultGeographicCRS.WGS84);
      } catch (FactoryException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Add a buffer around a geometry that makes sure that the buffer is the same distance (in
     * meters) anywhere on earth.
     * <p>
     * Background: If you call the regular buffer() method on a JTS geometry that uses WGS84 as the
     * coordinate reference system, the buffer will be accurate at the equator but will become more
     * and more elongated the farther north/south you go.
     * <p>
     * Taken from https://stackoverflow.com/questions/36455020
     */
    private Geometry preciseBuffer(Geometry geometry) {
      try {
        Geometry pGeom = JTS.transform(geometry, toTransform);
        Geometry pBufferedGeom = pGeom.buffer(distanceInMeters, 4, BufferParameters.CAP_FLAT);
        return JTS.transform(pBufferedGeom, fromTransform);
      } catch (TransformException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
