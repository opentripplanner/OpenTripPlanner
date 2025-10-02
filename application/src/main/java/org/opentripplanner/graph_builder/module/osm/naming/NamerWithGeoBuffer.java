package org.opentripplanner.graph_builder.module.osm.naming;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;

/**
 * Base class for namers that use a geo buffer to query geo features.
 */
public abstract class NamerWithGeoBuffer implements EdgeNamer {

  protected PreciseBuffer preciseBuffer;

  @Override
  public I18NString name(OsmEntity way) {
    return way.getAssumedName();
  }

  protected void postprocess(Collection<EdgeOnLevel> unnamedEdges, int bufferMeters, String type, Logger logger) {
    ProgressTracker progress = ProgressTracker.track(
      String.format("Assigning names to %s", type),
      500,
      unnamedEdges.size()
    );

    this.preciseBuffer = new PreciseBuffer(computeEnvelopeCenter(unnamedEdges), bufferMeters);

    final AtomicInteger namesApplied = new AtomicInteger(0);
    unnamedEdges
      .parallelStream()
      .forEach(edgeOnLevel -> {
        var buffer = preciseBuffer.preciseBuffer(edgeOnLevel.edge.getGeometry());
        if (assignNameToEdge(edgeOnLevel, buffer)) {
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
   * Implementation-specific logic for naming an edge.
   * @return true if a name was applied, false otherwise.
   */
  protected abstract boolean assignNameToEdge(EdgeOnLevel edgeOnLevel, Geometry buffer);

  /**
   * Compute the centroid of all sidewalk edges.
   */
  private Coordinate computeEnvelopeCenter(Collection<EdgeOnLevel> edges) {
    var envelope = new Envelope();
    edges.forEach(e -> {
      envelope.expandToInclude(e.edge.getFromVertex().getCoordinate());
      envelope.expandToInclude(e.edge.getToVertex().getCoordinate());
    });
    return envelope.centre();
  }

  /**
   * Adds an entry to a geospatial index.
   */
  protected static void addToSpatialIndex(OsmEntity way, StreetEdgePair pair, HashGridSpatialIndex<EdgeOnLevel> spatialIndex) {
    addToSpatialIndex(way, pair, spatialIndex, Integer.MAX_VALUE);
  }

  /**
   * Adds an entry to a geospatial index if its length is less than a threshold.
   */
  protected static void addToSpatialIndex(
    OsmEntity way,
    StreetEdgePair pair,
    HashGridSpatialIndex<EdgeOnLevel> spatialIndex,
    int maxLengthMeters
  ) {
    // We generate two edges for each osm way: one there and one back. This spatial index only
    // needs to contain one item for each road segment with a unique geometry and name, so we
    // add only one of the two edges.
    var edge = pair.pickAny();
    if (edge.getDistanceMeters() <= maxLengthMeters) {
      spatialIndex.insert(
        edge.getGeometry().getEnvelopeInternal(),
        new EdgeOnLevel((OsmWay)way, edge, way.getLevels())
      );
    }
  }

  public record EdgeOnLevel(OsmWay way, StreetEdge edge, Set<String> levels) {}

  /**
   * A class to cache the expensive construction of a Universal Traverse Mercator coordinate
   * reference system.
   * Re-using the same CRS for all edges might introduce tiny imprecisions for OTPs use cases
   * but speeds up the processing enormously and is a price well worth paying.
   */
  protected static final class PreciseBuffer {

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
    protected Geometry preciseBuffer(Geometry geometry) {
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
