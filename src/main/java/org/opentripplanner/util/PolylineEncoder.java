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

package org.opentripplanner.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.util.model.EncodedPolylineBean;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class PolylineEncoder {

    public static EncodedPolylineBean createEncodings(double[] lat, double[] lon) {
        return createEncodings(new PointAdapterList(lat, lon));
    }

    public static EncodedPolylineBean createEncodings(double[] lat, double[] lon, int level) {
        return createEncodings(new PointAdapterList(lat, lon), level);
    }

    public static EncodedPolylineBean createEncodings(double[] lat, double[] lon, int offset,
            int length, int level) {
        return createEncodings(new PointAdapterList(lat, lon, offset, length), level);
    }

    public static EncodedPolylineBean createEncodings(Iterable<Coordinate> points) {
        return createEncodings(points, -1);
    }

    public static EncodedPolylineBean createEncodings(Geometry geometry) {
        if (geometry instanceof LineString) {

            LineString string = (LineString) geometry;
            Coordinate[] coordinates = string.getCoordinates();
            return createEncodings(new CoordinateList(coordinates));
        } else if (geometry instanceof MultiLineString) {
            MultiLineString mls = (MultiLineString) geometry;
            return createEncodings(new CoordinateList(mls.getCoordinates()));
        } else {
            throw new IllegalArgumentException(geometry.toString());
        }
    }

    /**
     * If level < 0, then {@link EncodedPolylineBean#getLevels()} will be null.
     * 
     * @param points
     * @param level
     * @return
     */
    public static EncodedPolylineBean createEncodings(Iterable<Coordinate> points, int level) {

        StringBuilder encodedPoints = new StringBuilder();
        StringBuilder encodedLevels = new StringBuilder();

        int plat = 0;
        int plng = 0;
        int count = 0;

        for (Coordinate point : points) {

            int late5 = floor1e5(point.y);
            int lnge5 = floor1e5(point.x);

            int dlat = late5 - plat;
            int dlng = lnge5 - plng;

            plat = late5;
            plng = lnge5;

            encodedPoints.append(encodeSignedNumber(dlat)).append(encodeSignedNumber(dlng));
            if (level >= 0)
                encodedLevels.append(encodeNumber(level));
            count++;
        }

        String pointsString = encodedPoints.toString();
        String levelsString = level >= 0 ? encodedLevels.toString() : null;
        return new EncodedPolylineBean(pointsString, levelsString, count);
    }

    public static List<Coordinate> decode(EncodedPolylineBean polyline) {

        String pointString = polyline.getPoints();

        double lat = 0;
        double lon = 0;

        int strIndex = 0;
        List<Coordinate> points = new ArrayList<Coordinate>();

        while (strIndex < pointString.length()) {

            int[] rLat = decodeSignedNumberWithIndex(pointString, strIndex);
            lat = lat + rLat[0] * 1e-5;
            strIndex = rLat[1];

            int[] rLon = decodeSignedNumberWithIndex(pointString, strIndex);
            lon = lon + rLon[0] * 1e-5;
            strIndex = rLon[1];

            points.add(new Coordinate(lon, lat));
        }

        return points;
    }

    /*****************************************************************************
     * Private Methods
     ****************************************************************************/

    private static final int floor1e5(double coordinate) {
        return (int) Math.floor(coordinate * 1e5);
    }

    public static String encodeSignedNumber(int num) {
        int sgn_num = num << 1;
        if (num < 0) {
            sgn_num = ~(sgn_num);
        }
        return (encodeNumber(sgn_num));
    }

    public static int decodeSignedNumber(String value) {
        int[] r = decodeSignedNumberWithIndex(value, 0);
        return r[0];
    }

    public static int[] decodeSignedNumberWithIndex(String value, int index) {
        int[] r = decodeNumberWithIndex(value, index);
        int sgn_num = r[0];
        if ((sgn_num & 0x01) > 0) {
            sgn_num = ~(sgn_num);
        }
        r[0] = sgn_num >> 1;
        return r;
    }

    public static String encodeNumber(int num) {

        StringBuffer encodeString = new StringBuffer();

        while (num >= 0x20) {
            int nextValue = (0x20 | (num & 0x1f)) + 63;
            encodeString.append((char) (nextValue));
            num >>= 5;
        }

        num += 63;
        encodeString.append((char) (num));

        return encodeString.toString();
    }

    public static int decodeNumber(String value) {
        int[] r = decodeNumberWithIndex(value, 0);
        return r[0];
    }

    public static int[] decodeNumberWithIndex(String value, int index) {

        if (value.length() == 0)
            throw new IllegalArgumentException("string is empty");

        int num = 0;
        int v = 0;
        int shift = 0;

        do {
            v = value.charAt(index++) - 63;
            num |= (v & 0x1f) << shift;
            shift += 5;
        } while (v >= 0x20);

        return new int[] { num, index };
    }

    private static class PointAdapterList extends AbstractList<Coordinate> {

        private double[] _lat;
        private double[] _lon;
        private int _offset;
        private int _length;

        public PointAdapterList(double[] lat, double[] lon) {
            this(lat, lon, 0, lat.length);
        }

        public PointAdapterList(double[] lat, double[] lon, int offset, int length) {
            _lat = lat;
            _lon = lon;
            _offset = offset;
            _length = length;
        }

        @Override
        public Coordinate get(int index) {
            return new Coordinate(_lon[_offset + index], _lat[_offset + index]);
        }

        @Override
        public int size() {
            return _length;
        }
    }

    private static class CoordinateList extends AbstractList<Coordinate> {

        private Coordinate[] _coordinates;

        public CoordinateList(Coordinate[] coordinates) {
            _coordinates = coordinates;
        }

        @Override
        public Coordinate get(int index) {
            return _coordinates[index];
        }

        @Override
        public int size() {
            return _coordinates.length;
        }
    }
}
