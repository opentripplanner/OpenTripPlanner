package org.opentripplanner.common.geometry;

import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.linearref.LengthLocationMap;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.model.P2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class GeometryUtils {
    private static final Logger LOG = LoggerFactory.getLogger(GeometryUtils.class);

    private static CoordinateSequenceFactory csf = new Serializable2DPackedCoordinateSequenceFactory();
    private static GeometryFactory gf = new GeometryFactory(csf);

    /** A shared copy of the WGS84 CRS with longitude-first axis order. */
    public static final CoordinateReferenceSystem WGS84_XY;
    static {
        try {
            WGS84_XY = CRS.getAuthorityFactory(true).createCoordinateReferenceSystem("EPSG:4326");
        } catch (Exception ex) {
            LOG.error("Unable to create longitude-first WGS84 CRS", ex);
            throw new RuntimeException("Could not create longitude-first WGS84 coordinate reference system.");
        }
    }

    public static LineString makeLineString(double... coords) {
        GeometryFactory factory = getGeometryFactory();
        Coordinate [] coordinates = new Coordinate[coords.length / 2];
        for (int i = 0; i < coords.length; i+=2) {
            coordinates[i / 2] = new Coordinate(coords[i], coords[i+1]);
        }
        return factory.createLineString(coordinates);
    }

    public static LineString makeLineString(Coordinate[] coordinates) {
        GeometryFactory factory = getGeometryFactory();
        return factory.createLineString(coordinates);
    }

    public static LineString concatenateLineStrings(List<LineString> lineStrings) {
        GeometryFactory factory = getGeometryFactory();
        return factory.createLineString(
                lineStrings.stream()
                        .flatMap(t -> Arrays.stream(t.getCoordinates()))
                        .toArray(Coordinate[]::new));
    }

    public static LineString addStartEndCoordinatesToLineString(Coordinate startCoord, LineString lineString, Coordinate endCoord) {
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
    public static P2<LineString> splitGeometryAtPoint(Geometry geometry, Coordinate nearestPoint) {
        // An index in JTS can actually refer to any point along the line. It is NOT an array index.
        LocationIndexedLine line = new LocationIndexedLine(geometry);
        LinearLocation l = line.indexOf(nearestPoint);

        LineString beginning = (LineString) line.extractLine(line.getStartIndex(), l);
        LineString ending = (LineString) line.extractLine(l, line.getEndIndex());

        return new P2<LineString>(beginning, ending);
    }
    
    /**
     * Splits the input geometry into two LineStrings at a fraction of the distance covered.
     */
    public static P2<LineString> splitGeometryAtFraction(Geometry geometry, double fraction) {
        LineString empty = new LineString(null, gf);
        Coordinate[] coordinates = geometry.getCoordinates();
        CoordinateSequence sequence = gf.getCoordinateSequenceFactory().create(coordinates);
        LineString total = new LineString(sequence, gf);

        if (coordinates.length < 2) { return new P2<>(empty, empty); }
        if (fraction <= 0) { return new P2<>(empty, total); }
        if (fraction >= 1) { return new P2<>(total, empty); }

        double totalDistance = total.getLength();
        double requestedDistance = totalDistance * fraction;

        // An index in JTS can actually refer to any point along the line. It is NOT an array index.
        LocationIndexedLine line = new LocationIndexedLine(geometry);
        LinearLocation l = LengthLocationMap.getLocation(geometry, requestedDistance);

        LineString beginning = (LineString) line.extractLine(line.getStartIndex(), l);
        LineString ending = (LineString) line.extractLine(l, line.getEndIndex());

        return new P2<LineString>(beginning, ending);
    }

    /**
     * Returns the chunk of the given geometry between the two given coordinates.
     * 
     * Assumes that "second" is after "first" along the input geometry.
     */
    public static LineString getInteriorSegment(Geometry geomerty, Coordinate first,
            Coordinate second) {
        P2<LineString> splitGeom = GeometryUtils.splitGeometryAtPoint(geomerty, first);
        splitGeom = GeometryUtils.splitGeometryAtPoint(splitGeom.second, second);
        return splitGeom.first;
    }

    /**
     * Adapted from org.locationtech.jts.geom.LineSegment
     * Combines segmentFraction and projectionFactor methods.
     */
    public static double segmentFraction(double x0, double y0, double x1, double y1, 
            double xp, double yp, double xscale) {
        // Use comp.graphics.algorithms Frequently Asked Questions method
        double dx = (x1 - x0) * xscale;
        double dy = y1 - y0;
        double len2 = dx * dx + dy * dy;
        // this fixes a (reported) divide by zero bug in JTS when line segment has 0 length
        if (len2 == 0) {
            return 0;
        }
        double r = ( (xp - x0) * xscale * dx + (yp - y0) * dy ) / len2;
        if (r < 0.0) {
            return 0.0;
        }
        else if (r > 1.0) {
            return 1.0;
        }
        return r;
    }


    /**
     * Convert a org.geojson.Xxxx geometry to a JTS geometry.
     * Only support Point, Polygon and MultiPolygon for now.
     * @param geoJsonGeom
     * @return The equivalent JTS geometry.
     * @throws UnsupportedGeometryException
     */
    public static Geometry convertGeoJsonToJtsGeometry(GeoJsonObject geoJsonGeom)
        throws UnsupportedGeometryException {
        if (geoJsonGeom instanceof org.geojson.Point) {
            org.geojson.Point geoJsonPoint = (org.geojson.Point) geoJsonGeom;
            return gf.createPoint(new Coordinate(geoJsonPoint.getCoordinates().getLongitude(), geoJsonPoint
                .getCoordinates().getLatitude()));

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
                for (List<LngLatAlt> geoJsonRing : geoJsonRings)
                    geoJsonPoly.add(geoJsonRing);
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
                    geoJsonPath.toArray(new LngLatAlt[geoJsonPath.size()]));
                jtsLineStrings[i++] = (LineString) convertGeoJsonToJtsGeometry(geoJsonLineString);
            }
            return gf.createMultiLineString(jtsLineStrings);
        }

        throw new UnsupportedGeometryException(geoJsonGeom.getClass().toString());
    }

    private static Coordinate[] convertPath(List<LngLatAlt> path) {
        Coordinate[] coords = new Coordinate[path.size()];
        int i = 0;
        for (LngLatAlt p : path) {
            coords[i++] = new Coordinate(p.getLongitude(), p.getLatitude());
        }
        return coords;
    }
}
