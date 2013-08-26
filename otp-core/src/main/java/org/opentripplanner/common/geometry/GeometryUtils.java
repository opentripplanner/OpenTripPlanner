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

import org.opentripplanner.common.model.P2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

public class GeometryUtils {

    private static CoordinateSequenceFactory csf =
            new Serializable2DPackedCoordinateSequenceFactory();
    private static GeometryFactory gf = new GeometryFactory(csf);

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

        double fractionalIndex = binarySearchCoordinates(coordinates, requestedDistance);
        int lowIndex = (int) Math.floor(fractionalIndex);
        int highIndex = (int) Math.ceil(fractionalIndex);

        if (lowIndex == highIndex) {
            return splitGeometryAtPoint(geometry, coordinates[lowIndex]);
        } else {
            double lowFactor = highIndex - fractionalIndex;
            double highFactor = fractionalIndex - lowIndex;
            double x = coordinates[lowIndex].x * lowFactor + coordinates[highIndex].x * highFactor;
            double y = coordinates[lowIndex].y * lowFactor + coordinates[highIndex].y * highFactor;

            Coordinate splitCoordinate = new Coordinate(x, y);
            Coordinate[] beginning = new Coordinate[lowIndex + 2];
            Coordinate[] ending = new Coordinate[coordinates.length - lowIndex];

            for (int i = 0; i <= lowIndex; i++) {
                beginning[i] = coordinates[i];
            }
            beginning[lowIndex + 1] = splitCoordinate;

            CoordinateSequence firstSequence = gf.getCoordinateSequenceFactory().create(beginning);
            LineString firstLineString = new LineString(firstSequence, gf);

            for (int i = coordinates.length - 1; i >= highIndex; i--) {
                ending[i - lowIndex] = coordinates[i];
            }
            ending[0] = splitCoordinate;

            CoordinateSequence secondSequence = gf.getCoordinateSequenceFactory().create(ending);
            LineString secondLineString = new LineString(secondSequence, gf);

            return new P2<LineString>(firstLineString, secondLineString);
        }
    }

    /**
     * Returns the chunk of the given geometry between the two given coordinates.
     * 
     * Assumes that "second" is after "first" along the input geometry.
     */
    public static LineString getInteriorSegment(Geometry geomerty, Coordinate first,
            Coordinate second) {
        P2<LineString> splitGeom = GeometryUtils.splitGeometryAtPoint(geomerty, first);
        splitGeom = GeometryUtils.splitGeometryAtPoint(splitGeom.getSecond(), second);
        return splitGeom.getFirst();
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
     * Binary search method adapted from GNU Classpath Arrays.java (GPL).
     * Search across an array of Coordinate objects, computing the length of the geometry described.
     *
     * @return the index at which the distance is as requested, or a value in between two indices if
     * the match is not exact. In that case, the fractional part of the result will be proportional
     * to the distance between the two coordinates and the desired distance.
     */
    public static double binarySearchCoordinates(
            Coordinate[] coordinates, double requestedDistance) {
        if (coordinates.length < 2) return 0;

        int low = 0;
        int high = coordinates.length - 1;
        int middle;

        if (requestedDistance <= computePartialLength(coordinates, low)) return low;
        if (requestedDistance >= computePartialLength(coordinates, high)) return high;

        while (low < high - 1) {
            middle = (low + high) >>> 1;    // Shift right logical so the full range of int is used.
            double middleDistance = computePartialLength(coordinates, middle);

            if (requestedDistance > middleDistance) {
                low = middle;
            } else if (requestedDistance < middleDistance) {
                high = middle;
            } else {
                return middle;
            }
        }

        double lowDistance = computePartialLength(coordinates, low);
        double highDistance = computePartialLength(coordinates, high);
        double differenceHighLow = highDistance - lowDistance;
        double differenceRequestedLow = requestedDistance - lowDistance;

        return low + (differenceRequestedLow / differenceHighLow);
    }

    /**
     * Compute the length of part of a LineString object, built from an array of Coordinate objects.
     *
     * @return the length of a LineString, as built from the coordinates array, from start to index.
     */
    public static double computePartialLength(Coordinate[] coordinates, int index) {
        if (index < 1) return 0;    // A line string consisting of a single point has a length of 0.
        if (index >= coordinates.length) index = coordinates.length - 1;    // Check upper bound.

        Coordinate[] array = new Coordinate[index + 1];

        for (int i = 0; i <= index; i++) {
            array[i] = coordinates[i];
        }

        CoordinateSequence sequence = gf.getCoordinateSequenceFactory().create(array);
        LineString lineString = new LineString(sequence, gf);

        return lineString.getLength();
    }
}
