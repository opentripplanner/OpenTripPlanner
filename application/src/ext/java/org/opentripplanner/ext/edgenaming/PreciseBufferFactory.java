package org.opentripplanner.ext.edgenaming;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.buffer.BufferParameters;

/**
 * A class to cache the expensive construction of a Universal Traverse Mercator coordinate reference
 * system. Re-using the same CRS for all edges might introduce tiny imprecisions for OTPs use cases
 * but speeds up the processing enormously and is a price well worth paying.
 */
final class PreciseBufferFactory {

  private final double distanceInMeters;
  private final MathTransform toTransform;
  private final MathTransform fromTransform;

  PreciseBufferFactory(Coordinate coordinate, double distanceInMeters) {
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
   * Add a buffer around a geometry that makes sure that the buffer is the same distance (in meters)
   * anywhere on earth.
   * <p>
   * Background: If you call the regular buffer() method on a JTS geometry that uses WGS84 as the
   * coordinate reference system, the buffer will be accurate at the equator but will become more
   * and more elongated the farther north/south you go.
   * <p>
   * Taken from https://stackoverflow.com/questions/36455020
   */
  Geometry preciseBuffer(Geometry geometry) {
    try {
      Geometry pGeom = JTS.transform(geometry, toTransform);
      Geometry pBufferedGeom = pGeom.buffer(distanceInMeters, 4, BufferParameters.CAP_FLAT);
      return JTS.transform(pBufferedGeom, fromTransform);
    } catch (TransformException e) {
      throw new RuntimeException(e);
    }
  }
}
