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

import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.index.SpatialIndex;

/**
 * A spatial index using a 2D fast long hashtable (Trove lib).
 * 
 * Objects to index are placed in all grid bins touching the bounding envelope. We *do not store*
 * any bouding envelope for each object, so this imply that we will return false positive when
 * querying, and it's up to the client to filter them out (with whatever knowledge it has on the
 * location of the object).
 * 
 * Note: For performance reasons, write operation are not synchronized, it must be taken care by the
 * client. Read-only operation are multi-thread-safe though.
 * 
 * @author laurent
 * 
 * @param <T> Type of objects to be spatial indexed.
 */
public class HashGridSpatialIndex<T> implements SpatialIndex, Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(HashGridSpatialIndex.class);

    /* Computation done based on geographical coordinates. */
    private static final double DEFAULT_Y_BIN_SIZE = 0.010; // ~1km

    /* Computation done based on geographical coordinates at ~45 degree lat */
    private static final double DEFAULT_X_BIN_SIZE = 0.007; // ~1km

    /* Size of bin in X and Y direction, in coordinates units. */
    private final double xBinSize, yBinSize;

    /* The map of all bins. Please see visit() and xKey/yKey for details on the key. */
    private final TLongObjectHashMap<List<T>> bins;

    private int nBins = 0;

    private int nEntries = 0;

    public HashGridSpatialIndex(double xBinSize, double yBinSize) {
        if (xBinSize <= 0 || yBinSize <= 0)
            throw new IllegalStateException("bin size must be positive.");
        this.xBinSize = xBinSize;
        this.yBinSize = yBinSize;
        // For 200m bins, 500x500 = 100x100km = 250000 bins
        bins = new TLongObjectHashMap<>();
    }

    /** Create a HashGrid with the default grid dimensions. */
    public HashGridSpatialIndex() {
        this(DEFAULT_X_BIN_SIZE, DEFAULT_Y_BIN_SIZE);
    }

    @Override
    public final void insert(Envelope envelope, final Object item) {
        visit(envelope, true, new BinVisitor<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean visit(List<T> bin) {
                /*
                 * Note: here we can end-up having several time the same object in the same bin, if
                 * the client insert multiple times the same object with different envelopes.
                 * However we do filter duplicated when querying, so apart for memory/performance
                 * reasons it should work. If this becomes a problem, we can use a set instead of a
                 * list.
                 */
                bin.add((T) item);
                return false;
            }
        });
        nEntries++;
    }

    @Override
    public final List<T> query(Envelope envelope) {
        final Set<T> ret = new HashSet<>(1024);
        visit(envelope, false, new BinVisitor<T>() {
            @Override
            public boolean visit(List<T> bin) {
                ret.addAll(bin);
                return false;
            }
        });
        return new ArrayList<T>(ret);
    }

    @Override
    public final void query(Envelope envelope, ItemVisitor visitor) {
        // We are cheating a bit here... But who cares? Never called in OTP.
        List<T> tlist = query(envelope);
        for (T t : tlist) {
            visitor.visitItem(t);
        }
    }

    @Override
    public final boolean remove(Envelope envelope, Object item) {
        boolean retval = false;
        Coordinate min = new Coordinate(envelope.getMinX(), envelope.getMinY());
        Coordinate max = new Coordinate(envelope.getMaxX(), envelope.getMaxY());
        long minXKey = xKey(min.x);
        long maxXKey = xKey(max.x);
        long minYKey = yKey(min.y);
        long maxYKey = yKey(max.y);
        for (long xKey = minXKey; xKey <= maxXKey; xKey++) {
            for (long yKey = minYKey; yKey <= maxYKey; yKey++) {
                long mapKey = xKey << 32 | yKey;
                List<T> bin = bins.get(mapKey);
                if (bin != null) {
                    boolean removed = bin.remove(item);
                    if (removed)
                        retval = true;
                }
            }
        }
        if (retval)
            nEntries--;
        return retval;
    }

    private interface BinVisitor<T> {

        /**
         * Bin visitor callback.
         * @param bin
         * @return true if something has been removed from the bin.
         */
        abstract boolean visit(List<T> bin);
    }

    /**
     * Visit each bin touching the envelope.
     * @param envelope Self-descripting.
     * @param createIfEmpty Create a new bin if not existing.
     * @param binVisitor The callback to call for each visited bin.
     */
    private void visit(Envelope envelope, boolean createIfEmpty, final BinVisitor<T> binVisitor) {
        Coordinate min = new Coordinate(envelope.getMinX(), envelope.getMinY());
        Coordinate max = new Coordinate(envelope.getMaxX(), envelope.getMaxY());
        long minXKey = xKey(min.x);
        long maxXKey = xKey(max.x);
        long minYKey = yKey(min.y);
        long maxYKey = yKey(max.y);
        for (long xKey = minXKey; xKey <= maxXKey; xKey++) {
            for (long yKey = minYKey; yKey <= maxYKey; yKey++) {
                long mapKey = xKey << 32 | yKey;
                List<T> bin = bins.get(mapKey);
                if (createIfEmpty && bin == null) {
                    bin = new ArrayList<>();
                    bins.put(mapKey, bin);
                    nBins++;
                }
                if (bin != null) {
                    boolean modified = binVisitor.visit(bin);
                    if (modified && bin.isEmpty()) {
                        bins.remove(mapKey);
                        nBins--;
                    }
                }
            }
        }
    }

    /**
     * Compute the X index of the bin. Could overflow for really large coordinates: TODO wrap if needed.
     * @param x coordinate
     * @return index between 0 and Integer.MAX_VALUE
     */
    private final long xKey(double x) {
        long xKey = Math.round(x / xBinSize) + 0x8000000;
        // This should not happen for geographical uses,
        // even with ridiculously small bin size (>0.14m)
        if (xKey < 0 || xKey > Integer.MAX_VALUE)
            throw new RuntimeException("HashGrid bin X-index overflow, please use larger bin size.");
        return xKey;
    }

    /**
     * Compute the Y index of the bin.
     * @param y coordinate
     * @return index between 0 and Integer.MAX_VALUE
     */
    private final long yKey(double y) {
        long yKey = Math.round(y / yBinSize) + 0x8000000;
        // This should not happen for geographical uses,
        // even with ridiculously small bin size (>0.14m)
        if (yKey < 0 || yKey > Integer.MAX_VALUE)
            throw new RuntimeException("HashGrid bin Y-index overflow, please use larger bin size.");
        return yKey;
    }

    public String toString() {
        return String.format(
                "HashGridSpatialIndex %f x %f, %d bins allocated, %d objects (%.2f load factor)",
                this.xBinSize, this.yBinSize, this.nBins, this.nEntries, this.nEntries * 1.0
                        / this.nBins);
    }
}
