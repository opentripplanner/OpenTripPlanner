package org.opentripplanner.common.geometry;

import java.io.Serializable;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

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
     * The distance between samples in meters. Defaults to 10m, the approximate resolution of 1/3
     * arc-second NED data.
     */
    private static double distanceBetweenSamplesM = 10;

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

    /**
     * Compact an elevation profile onto a var-len int packed form (Dlugosz coding). This method
     * supposes that only the y-values are to be compacted and the x-values can be reconstructed
     * at regular intervals according to the distanceBetweenSamplesM field. The last x-value is given
     * by the length of the geometry.
     * 
     * @param profile The elevation profile to compact
     * @return The compacted format
     */
    public static byte[] compactElevationProfileWithRegularSamples(CoordinateSequence elevation) {
        if (elevation == null)
            return null;
        int oiy = 0;
        int[] coords = new int[elevation.size()];
        for (int i = 0; i < elevation.size(); i++) {
            /*
             * Note: We should do the rounding *before* the delta to prevent rounding errors from
             * accumulating on long line strings.
             */
            Coordinate c = elevation.getCoordinate(i);
            int iy = (int) Math.round(c.y * FIXED_FLOAT_MULT);
            int diy = iy - oiy;
            coords[i] = diy;
            oiy = iy;
        }
        return DlugoszVarLenIntPacker.pack(coords);
    }

    /**
     * Uncompact an ElevationProfile from a var-len int packed form (Dlugosz coding). This method
     * supposes that only the y-values have been compacted and x-values will be reconstructed at
     * regular interval according to the distanceBetweenSamplesM field. The last x-value is given
     * by the length of the geometry.
     *
     * @param packedCoords Compacted coordinates
     * @param lengthM The length of the edge in meters. This is used as the x-value of the final
     *                height sample
     * @return The elevation profile
     */
    public static PackedCoordinateSequence uncompactElevationProfileWithRegularSamples(byte[] packedCoords, double lengthM) {
        if (packedCoords == null)
            return null;
        int[] coords = DlugoszVarLenIntPacker.unpack(packedCoords);
        int size = coords.length;
        Coordinate[] c = new Coordinate[size];
        int oiy = 0;
        for (int i = 0; i < c.length; i++) {
            int iy = oiy + coords[i];
            c[i] = new Coordinate(
                    i == c.length - 1 ?
                            lengthM :
                            i * distanceBetweenSamplesM, iy / FIXED_FLOAT_MULT);
            oiy = iy;
        }
        return new PackedCoordinateSequence.Double(c, 2);
    }

    public static double getDistanceBetweenSamplesM() {
        return distanceBetweenSamplesM;
    }
}
