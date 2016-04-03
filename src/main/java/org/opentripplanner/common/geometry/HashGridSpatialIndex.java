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
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.opengis.referencing.cs.CoordinateSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
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
    // private static final double DEFAULT_Y_BIN_SIZE = 0.010; // ~1km
    private static final double DEFAULT_Y_BIN_SIZE = 0.005; // ~500m

    /* Computation done based on geographical coordinates at ~45 degree lat */
    // private static final double DEFAULT_X_BIN_SIZE = 0.007; // ~1km
    private static final double DEFAULT_X_BIN_SIZE = 0.0035; // ~500m

    /* Size of bin in X and Y direction, in coordinates units. */
    private final double xBinSize, yBinSize;

    /* The map of all bins. Please see visit() and xKey/yKey for details on the key. */
    private final TLongObjectHashMap<List<T>> bins;

    private int nBins = 0;

    private int nObjects = 0;

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
            public boolean visit(List<T> bin, long mapKey) {
                /*
                 * Note: here we can end-up having several time the same object in the same bin, if
                 * the client insert multiple times the same object with different envelopes.
                 * However we do filter duplicated when querying, so apart for memory/performance
                 * reasons it should work. If this becomes a problem, we can use a set instead of a
                 * list.
                 */
                bin.add((T) item);
                nEntries++;
                return false;
            }
        });
        nObjects++;
    }

    public final void insert(LineString geom, final Object item) {
        Coordinate[] coord = geom.getCoordinates();
        final TLongSet keys = new TLongHashSet(coord.length * 8);
        for (int i = 0; i < coord.length - 1; i++) {
            // TODO Cut the segment if longer than bin size
            // to reduce the number of wrong bins
            Envelope env = new Envelope(coord[i], coord[i + 1]);
            visit(env, true, new BinVisitor<T>() {
                @Override
                public boolean visit(List<T> bin, long mapKey) {
                    keys.add(mapKey);
                    return false;
                }
            });
        }
        keys.forEach(new TLongProcedure() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean execute(long key) {
                // Note: bins have been initialized in the previous visit
                bins.get(key).add((T) item);
                nEntries++;
                return true;
            }
        });
        nObjects++;
    }

    @Override
    public final List<T> query(Envelope envelope) {
        final Set<T> ret = new HashSet<>(1024);
        visit(envelope, false, new BinVisitor<T>() {
            @Override
            public boolean visit(List<T> bin, long mapKey) {
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
    public final boolean remove(Envelope envelope, final Object item) {
        final AtomicInteger removedCount = new AtomicInteger();
        visit(envelope, false, new BinVisitor<T>() {
            @Override
            public boolean visit(List<T> bin, long mapKey) {
                boolean removed = bin.remove(item);
                if (removed) {
                    nEntries--;
                    removedCount.addAndGet(1);
                }
                return removed;
            }
        });
        if (removedCount.get() > 0) {
            nObjects--;
            return true;
        } else {
            return false;
        }
    }

    private interface BinVisitor<T> {

        /**
         * Bin visitor callback.
         * 
         * @param bin
         * @return true if something has been removed from the bin.
         */
        abstract boolean visit(List<T> bin, long mapKey);
    }

    /** Clamp a coordinate to allowable lat/lon values */
    private static Coordinate clamp (Coordinate coord) {
        if (Math.abs(coord.x) > 180 || Math.abs(coord.y) > 90) {
            LOG.warn("Corner of envelope {} was invalid, clamping to valid range. Perhaps you're buffering something near a pole?", coord);

            // make a defensive copy as we're about to modify the coordinate
            coord = new Coordinate(coord);

            if (coord.x > 180) coord.x = 180;
            if (coord.x < -180) coord.x = -180;
            if (coord.y > 90) coord.y = 90;
            if (coord.y < -90) coord.y = -90;
        }

        return coord;
    }

    /**
     * Visit each bin touching the envelope.
     * 
     * @param envelope Self-descripting.
     * @param createIfEmpty Create a new bin if not existing.
     * @param binVisitor The callback to call for each visited bin.
     */
    private void visit(Envelope envelope, boolean createIfEmpty, final BinVisitor<T> binVisitor) {
        Coordinate min = new Coordinate(envelope.getMinX(), envelope.getMinY());
        Coordinate max = new Coordinate(envelope.getMaxX(), envelope.getMaxY());

        // clamp coordinates to earth. TODO: handle cross-date-line envelopes.
        min = clamp(min);
        max = clamp(max);

        long minXKey = Math.round(min.x / xBinSize);
        long maxXKey = Math.round(max.x / xBinSize);
        long minYKey = Math.round(min.y / yBinSize);
        long maxYKey = Math.round(max.y / yBinSize);
        for (long xKey = minXKey; xKey <= maxXKey; xKey++) {
            for (long yKey = minYKey; yKey <= maxYKey; yKey++) {
                /*
                 * For all known use, the average absolute value of x/y keys will be rather small
                 * compared to Integer.MAX_VALUE. We need to swap the two words (MSB and LSB) of
                 * xKey in order to have a well-behaving long hash, fitting in an int, because the
                 * default implementation is: hashInt = (int)(value ^ (value >>> 32));
                 */
                long mapKey = (yKey << 32) | ((xKey & 0xFFFF) << 16) | ((xKey >> 16) & 0xFFFF);
                List<T> bin = bins.get(mapKey);
                if (createIfEmpty && bin == null) {
                    bin = new ArrayList<>();
                    bins.put(mapKey, bin);
                    nBins++;
                }
                if (bin != null) {
                    boolean modified = binVisitor.visit(bin, mapKey);
                    if (modified && bin.isEmpty()) {
                        bins.remove(mapKey);
                        nBins--;
                    }
                }
            }
        }
    }

    public String toString() {
        return String
                .format("HashGridSpatialIndex %f x %f, %d bins allocated, %d objs, %d entries (avg %.2f entries/bin, %.2f entries/object)",
                        this.xBinSize, this.yBinSize, this.nBins, this.nObjects, this.nEntries,
                        this.nEntries * 1.0 / this.nBins, this.nEntries * 1.0 / this.nObjects);
    }
}
