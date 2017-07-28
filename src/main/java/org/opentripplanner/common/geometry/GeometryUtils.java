/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.common.geometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.linearref.LengthLocationMap;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opentripplanner.analyst.UnsupportedGeometryException;
import org.opentripplanner.common.model.P2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

        if (coordinates.length < 2) return new P2<LineString>(empty, empty);
        if (fraction <= 0) return new P2<LineString>(empty, total);
        if (fraction >= 1) return new P2<LineString>(total, empty);

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
     * Adapted from com.vividsolutions.jts.geom.LineSegment 
     * Combines segmentFraction and projectionFactor methods.
     */
    public static double segmentFraction(double x0, double y0, double x1, double y1, 
            double xp, double yp, double xscale) {
        // Use comp.graphics.algorithms Frequently Asked Questions method
        double dx = (x1 - x0) * xscale;
        double dy = y1 - y0;
        double len2 = dx * dx + dy * dy;
        // this fixes a (reported) divide by zero bug in JTS when line segment has 0 length
        if (len2 == 0)
            return 0;
        double r = ( (xp - x0) * xscale * dx + (yp - y0) * dy ) / len2;
        if (r < 0.0)
            return 0.0;
        else if (r > 1.0)
            return 1.0;
        return r;
    }

    /**
     * Convert a org.geojson.Xxxx geometry to a JTS geometry.
     * Only support Point, Polygon, MultiPolygon, LineString and MultiLineString for now.
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

    public static double getLengthInMeters(LineString lineString) {
        GeodeticCalculator calculator = new GeodeticCalculator(WGS84_XY);
        double length = 0d;
        try {
            for (int i = 0; i < lineString.getCoordinates().length - 1; i++) {
                Coordinate fromCoord = lineString.getCoordinates()[i];
                Coordinate toCoord = lineString.getCoordinates()[i + 1];
                calculator.setStartingPosition(JTS.toDirectPosition(fromCoord, WGS84_XY));
                calculator.setDestinationPosition(JTS.toDirectPosition(toCoord, WGS84_XY));
                double incrementalDistance = calculator.getOrthodromicDistance();
                length += incrementalDistance;
            }
        }catch (TransformException tfe){
            throw new RuntimeException(tfe.getMessage());
        }
        return length;
    }

    private static Coordinate[] convertPath(List<LngLatAlt> path) {
        Coordinate[] coords = new Coordinate[path.size()];
        int i = 0;
        for (LngLatAlt p : path) {
            coords[i++] = new Coordinate(p.getLatitude(), p.getLongitude());
        }
        return coords;
    }

    public static Geometry shiftLineByPerpendicularVector(LineString line, double distance, boolean reverse) {
        // restrict coordinates to line segments that are sufficiently long
        List<Coordinate> coordList = new ArrayList<>();
        coordList.add(line.getCoordinateN(0));
        for (int i = 1; i < line.getNumPoints(); i++) {
            Coordinate coord = line.getCoordinateN(i);
            if (SphericalDistanceLibrary.fastDistance(coordList.get(i - 1), coord) > 0.01) {
                coordList.add(coord);
            }
        }

        Coordinate[] p = coordList.toArray(new Coordinate[0]);
        int nPoints = p.length;
        LineSegment[] segments = new LineSegment[nPoints + 1];

        try {
            segments[0] = makeLineSegment(p[0], getComplementaryAngle(p[0], p[1], reverse), distance);
            for (int i = 0; i < nPoints - 1; i++) {
                segments[i + 1] = makeParallelLineSegment(p[i], p[i+1], distance, reverse);
            }
            segments[nPoints] = makeLineSegment(p[nPoints-1], getComplementaryAngle(p[nPoints-2], p[nPoints-1], reverse), distance);
        } catch (TransformException tfe) {
            tfe.printStackTrace();
            throw new RuntimeException(tfe.getMessage());
        }

        List<Coordinate> coords = new ArrayList<>();
        for (int i = 0; i < nPoints; i++) {
            Coordinate coord;
            double angleDiff = Math.abs(segments[i].angle() - segments[i + 1].angle());
            // if segments are sufficiently parallel use an endpoint
            if (angleDiff < 0.000001) {
                coord = segments[i].getCoordinate(1);
            } else {
                coord = segments[i].lineIntersection(segments[i + 1]);
            }
            // sanity check
            if (coord != null && SphericalDistanceLibrary.fastDistance(coord, p[i]) < (10.0 * distance)) {
                coords.add(coord);
            } else {
                LOG.error("Error shifting line segment {}", line);
            }
        }
        return getGeometryFactory().createLineString(coords.toArray(new Coordinate[0]));
    }

    private static LineSegment makeParallelLineSegment(Coordinate p0, Coordinate p1, double distance, boolean reverse) throws TransformException {
        double angle = getComplementaryAngle(p0, p1, reverse);
        Coordinate s0 = shiftCoordinateByAngle(p0, angle, distance);
        Coordinate s1 = shiftCoordinateByAngle(p1, angle, distance);
        return new LineSegment(s0, s1);
    }

    private static Coordinate shiftCoordinateByAngle(Coordinate p0, double angle, double distance) throws TransformException {
        GeodeticCalculator calculator = new GeodeticCalculator(WGS84_XY);
        calculator.setStartingPosition(JTS.toDirectPosition(p0, WGS84_XY));
        calculator.setDirection(angle, distance);
        DirectPosition pos = calculator.getDestinationPosition();
        return JTS.toGeometry(pos).getCoordinate();
    }

    private static LineSegment makeLineSegment(Coordinate p0, double angle, double distance)
            throws TransformException {
        Coordinate p1 = shiftCoordinateByAngle(p0, angle, distance);
        return new LineSegment(p0, p1);
    }

    private static double getComplementaryAngle(Coordinate p0, Coordinate p1, boolean reverse) throws TransformException {
        GeodeticCalculator calculator = new GeodeticCalculator(WGS84_XY);
        calculator.setStartingPosition(JTS.toDirectPosition(p0, WGS84_XY));
        calculator.setDestinationPosition(JTS.toDirectPosition(p1, WGS84_XY));
        double angle = calculator.getAzimuth();
        angle += (reverse ? -90 : 90);
        while (angle >= 180d)
            angle -= 360d;
        while (angle <= -180d)
            angle += 360d;
        return angle;
    }

}
