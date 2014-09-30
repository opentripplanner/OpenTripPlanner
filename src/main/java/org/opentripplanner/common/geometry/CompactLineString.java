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

import java.io.Serializable;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Compact line string. To optimize storage, we use the following tricks:
 * <ul>
 * <li>Store only intermediate points (end-points are given by the external context, ie from/to
 * vertices)</li>
 * <li>For straight-line geometries (sometimes around half of the street geometries), re-use the
 * same static object (since there is nothing to store)</li>
 * <li>Store intermediate point in fixed floating points with fixed precision, using delta coding
 * from the previous point, and variable length coding (most of the delta coordinates will thus fits
 * in 1 or 2 bytes).</li>
 * </ul>
 * 
 * This trick alone saves around 20% of memory compared to the bulky JTS LineString, which is split
 * on many objects (Coordinates, cached Envelope, Geometry itself). Performance hit should be low as
 * we do not need the geometry during a path search.
 * 
 * @author laurent
 */
public final class CompactLineString implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Multiplier for fixed-float representation. For lat/lon CRS, 1e6 leads to a precision of 0.11
     * meter at a minimum (at the equator).
     */
    private static final double FIXED_FLOAT_MULT = 1.0e6;

    /**
     * Constant to check that line string end points are sticking to given points. 0.000001 is
     * around 1 meter at the equator. Do not use a too low value, ShapeFile builder has some
     * rounding issues and do not ensure perfect equality between endpoints and geometry.
     */
    private static final double EPS = 0.000001;

    /**
     * Singleton representation of a straight-line (where nothing has to be stored), to be re-used.
     */
    protected static final int[] STRAIGHT_LINE = new int[0];

    protected static final byte[] STRAIGHT_LINE_PACKED = new byte[0];

    /**
     * Geometry factory. TODO - Do we need to make this parametrable?
     */
    private static GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * Public factory to create a compact line string. Optimize for straight-line only line string
     * (w/o intermediate end-points).
     * 
     * @param xa X coordinate of end point A
     * @param ya Y coordinate of end point A
     * @param xb X coordinate of end point B
     * @param yb Y coordinate of end point B
     * @param lineString The geometry to compact. Please be aware that we ignore first and last
     *        coordinate in the line string, they need to be exactly the same as A and B.
     * @param reverse True if A and B are inverted (B is start, A is end).
     * @return
     */
    public static int[] compactLineString(double xa, double ya, double xb, double yb,
            LineString lineString, boolean reverse) {
        if (lineString == null)
            return null;
        if (lineString.getCoordinates().length == 2)
            return STRAIGHT_LINE;
        double x0 = reverse ? xb : xa;
        double y0 = reverse ? yb : ya;
        double x1 = reverse ? xa : xb;
        double y1 = reverse ? ya : yb;
        Coordinate[] c = lineString.getCoordinates();
        /*
         * Check if the lineString is really sticking to the given end-points. TODO: If this is not
         * guaranteed, store all delta (from 0 to n-1) -- but how to mark it? A prefix?
         */
        if (Math.abs(x0 - c[0].x) > EPS || Math.abs(y0 - c[0].y) > EPS
                || Math.abs(x1 - c[c.length - 1].x) > EPS || Math.abs(y1 - c[c.length - 1].y) > EPS)
            throw new IllegalArgumentException(
                    "CompactLineString geometry must stick to given end points. If you need to relax this, please read source code.");
        int oix = (int) Math.round(x0 * FIXED_FLOAT_MULT);
        int oiy = (int) Math.round(y0 * FIXED_FLOAT_MULT);
        int[] coords = new int[(c.length - 2) * 2];
        for (int i = 1; i < c.length - 1; i++) {
            /*
             * Note: We should do the rounding *before* the delta to prevent rounding errors from
             * accumulating on long line strings.
             */
            int ix = (int) Math.round(c[i].x * FIXED_FLOAT_MULT);
            int iy = (int) Math.round(c[i].y * FIXED_FLOAT_MULT);
            int dix = ix - oix;
            int diy = iy - oiy;
            coords[(i - 1) * 2] = dix;
            coords[(i - 1) * 2 + 1] = diy;
            oix = ix;
            oiy = iy;
        }
        return coords;
    }

    /**
     * Same as the other version, but in a var-len int packed form (Dlugosz coding).
     * 
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param lineString
     * @return
     */
    public static byte[] compackLineString(double x0, double y0, double x1, double y1,
            LineString lineString, boolean reverse) {
        int[] coords = compactLineString(x0, y0, x1, y1, lineString, reverse);
        if (coords == STRAIGHT_LINE)
            return STRAIGHT_LINE_PACKED;
        return DlugoszVarLenIntPacker.pack(coords);
    }

    /**
     * Construct a LineString based on external end-points and compacted int version.
     * 
     * @param xa
     * @param ya
     * @param xb
     * @param yb
     * @param coords Compact version of coordinates
     * @param reverse True if A and B and the compacted geometry is reversed.
     * @return
     */
    public static LineString uncompactLineString(double xa, double ya, double xb, double yb,
            int[] coords, boolean reverse) {
        int size = coords == null ? 2 : (coords.length / 2) + 2;
        Coordinate[] c = new Coordinate[size];
        double x0 = reverse ? xb : xa;
        double y0 = reverse ? yb : ya;
        double x1 = reverse ? xa : xb;
        double y1 = reverse ? ya : yb;
        c[0] = new Coordinate(x0, y0);
        if (coords != null) {
            int oix = (int) Math.round(x0 * FIXED_FLOAT_MULT);
            int oiy = (int) Math.round(y0 * FIXED_FLOAT_MULT);
            for (int i = 1; i < c.length - 1; i++) {
                int ix = oix + coords[(i - 1) * 2];
                int iy = oiy + coords[(i - 1) * 2 + 1];
                c[i] = new Coordinate(ix / FIXED_FLOAT_MULT, iy / FIXED_FLOAT_MULT);
                oix = ix;
                oiy = iy;
            }
        }
        c[c.length - 1] = new Coordinate(x1, y1);
        LineString out = geometryFactory.createLineString(c);
        if (reverse)
            out = (LineString) out.reverse();
        return out;
    }

    /**
     * Same as the other version, but in a var-len int packed form (Dlugosz coding).
     * 
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param coords
     * @return
     */
    public static LineString uncompackLineString(double x0, double y0, double x1, double y1,
            byte[] packedCoords, boolean reverse) {
        return uncompactLineString(x0, y0, x1, y1, DlugoszVarLenIntPacker.unpack(packedCoords), reverse);
    }
}
