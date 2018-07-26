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

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.linearref.LengthLocationMap;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import org.geojson.LngLatAlt;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.model.P2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static Coordinate[] convertPath(List<LngLatAlt> path) {
        Coordinate[] coords = new Coordinate[path.size()];
        int i = 0;
        for (LngLatAlt p : path) {
            coords[i++] = new Coordinate(p.getLatitude(), p.getLongitude());
        }
        return coords;
    }
}
