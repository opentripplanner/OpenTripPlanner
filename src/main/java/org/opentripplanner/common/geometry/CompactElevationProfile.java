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
import com.vividsolutions.jts.geom.CoordinateSequence;

/**
 * Compact elevation profile. To optimize storage, we use the following tricks:
 * <ul>
 * <li>Store intermediate point in fixed floating points with fixed precision, using delta coding
 * from the previous point, and variable length coding (most of the delta coordinates will thus fits
 * in 1 or 2 bytes).</li>
 * </ul>
 * 
 * Performance hit should be low as we do not need the elevation profile itself during a path
 * search.
 * 
 * @author laurent
 */
public final class CompactElevationProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Multipler for fixed-float representation. In meters, the precision is 1 cm (elevation and arc
     * length).
     */
    private static final double FIXED_FLOAT_MULT = 1.0e2;

    /**
     * Compact an elevation profile onto a var-len int packed form (Dlugosz coding).
     * 
     * @param profile The elevation profile to compact
     * @return The compacted format
     */
    public static byte[] compactElevationProfile(CoordinateSequence elevation) {
        if (elevation == null)
            return null;
        int oix = 0;
        int oiy = 0;
        int[] coords = new int[elevation.size() * 2];
        for (int i = 0; i < elevation.size(); i++) {
            /*
             * Note: We should do the rounding *before* the delta to prevent rounding errors from
             * accumulating on long line strings.
             */
            Coordinate c = elevation.getCoordinate(i);
            int ix = (int) Math.round(c.x * FIXED_FLOAT_MULT);
            int iy = (int) Math.round(c.y * FIXED_FLOAT_MULT);
            int dix = ix - oix;
            int diy = iy - oiy;
            coords[i * 2] = dix;
            coords[i * 2 + 1] = diy;
            oix = ix;
            oiy = iy;
        }
        return DlugoszVarLenIntPacker.pack(coords);
    }

    /**
     * Uncompact an ElevationProfile from a var-len int packed form (Dlugosz coding).
     * 
     * TODO relax the returned type to CoordinateSequence
     * 
     * @param packedCoords Compacted coordinates
     * @return The elevation profile
     */
    public static PackedCoordinateSequence uncompactElevationProfile(byte[] packedCoords) {
        if (packedCoords == null)
            return null;
        int[] coords = DlugoszVarLenIntPacker.unpack(packedCoords);
        int size = coords.length / 2;
        Coordinate[] c = new Coordinate[size];
        int oix = 0;
        int oiy = 0;
        for (int i = 0; i < c.length; i++) {
            int ix = oix + coords[i * 2];
            int iy = oiy + coords[i * 2 + 1];
            c[i] = new Coordinate(ix / FIXED_FLOAT_MULT, iy / FIXED_FLOAT_MULT);
            oix = ix;
            oiy = iy;
        }
        return new PackedCoordinateSequence.Double(c, 2);
    }
}
