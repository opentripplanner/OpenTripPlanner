package org.opentripplanner.framework.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.linearref.LengthLocationMap;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

public class GeometryUtils {

  private static final CoordinateSequenceFactory csf = new PackedCoordinateSequenceFactory();
  private static final GeometryFactory gf = new GeometryFactory(csf);

  public static <T> Geometry makeConvexHull(
    Collection<T> collection,
    Function<T, Coordinate> mapToCoordinate
  ) {
    var gf = getGeometryFactory();
    Geometry[] points = new Geometry[collection.size()];
    int i = 0;
    for (T v : collection) {
      points[i++] = gf.createPoint(mapToCoordinate.apply(v));
    }

    var col = new GeometryCollection(points, gf);
    return new ConvexHull(col).getConvexHull();
  }

  public static LineString makeLineString(double... coords) {
    GeometryFactory factory = getGeometryFactory();
    Coordinate[] coordinates = new Coordinate[coords.length / 2];
    for (int i = 0; i < coords.length; i += 2) {
      coordinates[i / 2] = new Coordinate(coords[i], coords[i + 1]);
    }
    return factory.createLineString(coordinates);
  }

  public static LineString makeLineString(List<Coordinate> coordinates) {
    GeometryFactory factory = getGeometryFactory();
    return factory.createLineString(coordinates.toArray(new Coordinate[] {}));
  }

  public static LineString makeLineString(Coordinate... coordinates) {
    GeometryFactory factory = getGeometryFactory();
    return factory.createLineString(coordinates);
  }

  public static LineString makeLineString(WgsCoordinate... coordinates) {
    return makeLineString(Arrays.stream(coordinates).map(WgsCoordinate::asJtsCoordinate).toList());
  }

  public static <T> LineString concatenateLineStrings(
    List<T> inputObjects,
    Function<T, LineString> mapper
  ) {
    return concatenateLineStrings(inputObjects.stream().map(mapper).toList());
  }

  public static LineString concatenateLineStrings(List<LineString> lineStrings) {
    GeometryFactory factory = getGeometryFactory();
    Predicate<Coordinate[]> nonZeroLength = coordinates -> coordinates.length != 0;
    return factory.createLineString(
      lineStrings
        .stream()
        .filter(Objects::nonNull)
        .map(LineString::getCoordinates)
        .filter(nonZeroLength)
        .<CoordinateArrayListSequence>collect(
          CoordinateArrayListSequence::new,
          (acc, segment) -> {
            if ((acc.size() == 0 || !acc.getCoordinate(acc.size() - 1).equals(segment[0]))) {
              acc.extend(segment);
            } else {
              acc.extend(segment, 1);
            }
          },
          (head, tail) -> head.extend(tail.toCoordinateArray())
        )
    );
  }

  public static LineString addStartEndCoordinatesToLineString(
    Coordinate startCoord,
    LineString lineString,
    Coordinate endCoord
  ) {
    Coordinate[] coordinates = new Coordinate[lineString.getCoordinates().length + 2];
    coordinates[0] = startCoord;
    for (int j = 0; j < lineString.getCoordinates().length; j++) {
      coordinates[j + 1] = lineString.getCoordinates()[j];
    }
    coordinates[lineString.getCoordinates().length + 1] = endCoord;
    return makeLineString(coordinates);
  }

  public static LineString removeStartEndCoordinatesFromLineString(LineString lineString) {
    Coordinate[] coordinates = new Coordinate[lineString.getCoordinates().length - 2];
    for (int j = 1; j < lineString.getCoordinates().length - 1; j++) {
      coordinates[j - 1] = lineString.getCoordinates()[j];
    }
    return makeLineString(coordinates);
  }

  public static GeometryFactory getGeometryFactory() {
    return gf;
  }

  /**
   * Splits the input geometry into two LineStrings at the given point.
   */
  public static SplitLineString splitGeometryAtPoint(Geometry geometry, Coordinate nearestPoint) {
    // An index in JTS can actually refer to any point along the line. It is NOT an array index.
    LocationIndexedLine line = new LocationIndexedLine(geometry);
    LinearLocation l = line.indexOf(nearestPoint);

    LineString beginning = (LineString) line.extractLine(line.getStartIndex(), l);
    LineString ending = (LineString) line.extractLine(l, line.getEndIndex());

    return new SplitLineString(beginning, ending);
  }

  /**
   * Splits the input geometry into two LineStrings at a fraction of the distance covered.
   */
  public static SplitLineString splitGeometryAtFraction(Geometry geometry, double fraction) {
    LineString empty = new LineString(null, gf);
    Coordinate[] coordinates = geometry.getCoordinates();
    CoordinateSequence sequence = gf.getCoordinateSequenceFactory().create(coordinates);
    LineString total = new LineString(sequence, gf);

    if (coordinates.length < 2) {
      return new SplitLineString(empty, empty);
    }
    if (fraction <= 0) {
      return new SplitLineString(empty, total);
    }
    if (fraction >= 1) {
      return new SplitLineString(total, empty);
    }

    double totalDistance = total.getLength();
    double requestedDistance = totalDistance * fraction;

    // An index in JTS can actually refer to any point along the line. It is NOT an array index.
    LocationIndexedLine line = new LocationIndexedLine(geometry);
    LinearLocation l = LengthLocationMap.getLocation(geometry, requestedDistance);

    LineString beginning = (LineString) line.extractLine(line.getStartIndex(), l);
    LineString ending = (LineString) line.extractLine(l, line.getEndIndex());

    return new SplitLineString(beginning, ending);
  }

  /**
   * Returns the chunk of the given geometry between the two given coordinates.
   * <p>
   * Assumes that "second" is after "first" along the input geometry.
   */
  public static LineString getInteriorSegment(
    Geometry geomerty,
    Coordinate first,
    Coordinate second
  ) {
    SplitLineString splitGeom = GeometryUtils.splitGeometryAtPoint(geomerty, first);
    splitGeom = GeometryUtils.splitGeometryAtPoint(splitGeom.ending(), second);
    return splitGeom.beginning();
  }

  // TODO OTP2 move this method to a separate mapper class
  /**
   * Convert a org.geojson.Xxxx geometry to a JTS geometry. Only support Point, Polygon and
   * MultiPolygon for now.
   *
   * @return The equivalent JTS geometry.
   */
  public static Geometry convertGeoJsonToJtsGeometry(GeoJsonObject geoJsonGeom)
    throws UnsupportedGeometryException {
    if (geoJsonGeom instanceof org.geojson.Point) {
      org.geojson.Point geoJsonPoint = (org.geojson.Point) geoJsonGeom;
      return gf.createPoint(
        new Coordinate(
          geoJsonPoint.getCoordinates().getLongitude(),
          geoJsonPoint.getCoordinates().getLatitude(),
          geoJsonPoint.getCoordinates().getAltitude()
        )
      );
    } else if (geoJsonGeom instanceof org.geojson.Polygon) {
      org.geojson.Polygon geoJsonPolygon = (org.geojson.Polygon) geoJsonGeom;
      LinearRing shell = gf.createLinearRing(convertPath(geoJsonPolygon.getExteriorRing()));
      LinearRing[] holes = new LinearRing[geoJsonPolygon.getInteriorRings().size()];
      int i = 0;
      for (List<LngLatAlt> hole : geoJsonPolygon.getInteriorRings()) {
        holes[i++] = gf.createLinearRing(convertPath(hole));
      }
      return gf.createPolygon(shell, holes);
    } else if (geoJsonGeom instanceof org.geojson.MultiPolygon) {
      org.geojson.MultiPolygon geoJsonMultiPolygon = (org.geojson.MultiPolygon) geoJsonGeom;
      Polygon[] jtsPolygons = new Polygon[geoJsonMultiPolygon.getCoordinates().size()];
      int i = 0;
      for (List<List<LngLatAlt>> geoJsonRings : geoJsonMultiPolygon.getCoordinates()) {
        org.geojson.Polygon geoJsonPoly = new org.geojson.Polygon();
        for (List<LngLatAlt> geoJsonRing : geoJsonRings) geoJsonPoly.add(geoJsonRing);
        jtsPolygons[i++] = (Polygon) convertGeoJsonToJtsGeometry(geoJsonPoly);
      }
      return gf.createMultiPolygon(jtsPolygons);
    } else if (geoJsonGeom instanceof org.geojson.LineString) {
      org.geojson.LineString geoJsonLineString = (org.geojson.LineString) geoJsonGeom;
      return gf.createLineString(convertPath(geoJsonLineString.getCoordinates()));
    } else if (geoJsonGeom instanceof org.geojson.MultiLineString) {
      org.geojson.MultiLineString geoJsonMultiLineString = (org.geojson.MultiLineString) geoJsonGeom;
      LineString[] jtsLineStrings = new LineString[geoJsonMultiLineString.getCoordinates().size()];
      int i = 0;
      for (List<LngLatAlt> geoJsonPath : geoJsonMultiLineString.getCoordinates()) {
        org.geojson.LineString geoJsonLineString = new org.geojson.LineString(
          geoJsonPath.toArray(new LngLatAlt[0])
        );
        jtsLineStrings[i++] = (LineString) convertGeoJsonToJtsGeometry(geoJsonLineString);
      }
      return gf.createMultiLineString(jtsLineStrings);
    }

    throw new UnsupportedGeometryException(geoJsonGeom.getClass().toString());
  }

  /**
   * Extract individual line strings from a multi-line string.
   */
  public static List<LineString> getLineStrings(MultiLineString mls) {
    var ret = new ArrayList<LineString>();
    for (var i = 0; i < mls.getNumGeometries(); i++) {
      ret.add((LineString) mls.getGeometryN(i));
    }
    return List.copyOf(ret);
  }

  private static Coordinate[] convertPath(List<LngLatAlt> path) {
    Coordinate[] coords = new Coordinate[path.size()];
    int i = 0;
    for (LngLatAlt p : path) {
      // the serialization library does serialize a 0 but not a NaN
      coords[i++] = new Coordinate(p.getLongitude(), p.getLatitude(), p.getAltitude());
    }
    return coords;
  }

  /**
   * Split a linestring into its constituent segments and convert each into an envelope.
   * <p>
   * All segments form the complete line string again so [A,B,C,D] will be split into the
   * segments [[A,B],[B,C],[C,D]].
   */
  public static Stream<Envelope> toEnvelopes(LineString ls) {
    Coordinate[] coordinates = ls.getCoordinates();
    Envelope[] envelopes = new Envelope[coordinates.length - 1];

    for (int i = 0; i < envelopes.length; i++) {
      Coordinate from = coordinates[i];
      Coordinate to = coordinates[i + 1];
      envelopes[i] = new Envelope(from, to);
    }

    return Arrays.stream(envelopes);
  }

  /**
   * Returns the sum of the distances in between the pairs of coordinates in meters.
   */
  public static double sumDistances(List<Coordinate> coordinates) {
    double distance = 0;
    for (int i = 1; i < coordinates.size(); i++) {
      distance += SphericalDistanceLibrary.distance(coordinates.get(i - 1), coordinates.get(i));
    }
    return distance;
  }
}
