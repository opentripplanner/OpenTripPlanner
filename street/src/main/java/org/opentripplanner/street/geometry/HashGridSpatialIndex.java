package org.opentripplanner.street.geometry;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.index.SpatialIndex;
import org.opentripplanner.utils.lang.IntBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A spatial index using a 2D fast long hashtable (Trove lib).
 * <p>
 * Objects to index are placed in all grid bins touching the bounding envelope. We *do not store*
 * any bouding envelope for each object, so this imply that we will return false positive when
 * querying, and it's up to the client to filter them out (with whatever knowledge it has on the
 * location of the object).
 * <p>
 * Note: For performance reasons, write operation are not synchronized, it must be taken care by the
 * client. Read-only operation are multi-thread-safe though.
 *
 * @param <T> Type of objects to be spatial indexed.
 * @author laurent
 */
public class HashGridSpatialIndex<T> implements SpatialIndex, Serializable {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(HashGridSpatialIndex.class);

  /**
   * Computation done based on geographical coordinates. Default value is ~500 meters.
   */
  private static final double DEFAULT_Y_BIN_SIZE = 0.005;

  /**
   * Computation done based on geographical coordinates at ~45 degree lat. Default value is ~500
   * meters.
   */
  private static final double DEFAULT_X_BIN_SIZE = 0.0035;

  /* Size of bin in X and Y direction, in coordinates units. */
  private final double xBinSize;
  private final double yBinSize;

  /* The map of all bins. Please see visit() and xKey/yKey for details on the key. */
  private final TLongObjectHashMap<ArrayList<T>> bins;

  private int nBins = 0;

  private int nObjects = 0;

  private int nEntries = 0;

  public HashGridSpatialIndex(double xBinSize, double yBinSize) {
    if (xBinSize <= 0 || yBinSize <= 0) {
      throw new IllegalStateException("bin size must be positive.");
    }
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
    visit(envelope, true, (bin, mapKey) -> {
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
    });
    nObjects++;
  }

  @Override
  public final List<T> query(Envelope envelope) {
    final Set<T> ret = new HashSet<>(1024);
    visit(envelope, false, (bin, mapKey) -> {
      ret.addAll(bin);
      return false;
    });
    return new ArrayList<>(ret);
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
    // This iterates over the entire rectangular envelope of the edge rather than the segments
    // making it up. It will be inefficient for very long edges, but creating a new remove method
    // mirroring the more efficient insert logic is not trivial and would require additional
    // testing of the spatial index.
    final IntBox removedCount = new IntBox(0);
    visit(envelope, false, (bin, mapKey) -> {
      boolean removed = bin.remove(item);
      if (removed) {
        nEntries--;
        removedCount.inc();
      }
      return removed;
    });
    if (removedCount.get() > 0) {
      nObjects--;
      return true;
    } else {
      return false;
    }
  }

  public final void insert(LineString geom, final Object item) {
    Coordinate[] coord = geom.getCoordinates();
    final TLongSet keys = new TLongHashSet(coord.length * 8);
    for (int i = 0; i < coord.length - 1; i++) {
      // TODO Cut the segment if longer than bin size
      // to reduce the number of wrong bins
      Envelope env = new Envelope(coord[i], coord[i + 1]);
      visit(env, true, (bin, mapKey) -> {
        keys.add(mapKey);
        return false;
      });
    }
    keys.forEach(key -> {
      // Note: bins have been initialized in the previous visit
      bins.get(key).add((T) item);
      nEntries++;
      return true;
    });
    nObjects++;
  }

  /**
   * Query all items near any segment of the given line strings. Collects the unique set of bin
   * keys touched by every segment, then reads each bin exactly once. Avoids per-segment
   * {@link Envelope} and lambda allocations, and the re-read of bins shared by consecutive
   * segments (which is the common case — adjacent segments always share at least one bin via
   * their shared endpoint).
   */
  public Set<T> queryAlongLineStrings(Collection<LineString> lineStrings) {
    Set<T> result = new HashSet<>(1024);
    TLongSet keys = new TLongHashSet(256);
    for (LineString ls : lineStrings) {
      CoordinateSequence seq = ls.getCoordinateSequence();
      for (int i = 0; i < seq.size() - 1; i++) {
        collectBinKeys(seq.getX(i), seq.getY(i), seq.getX(i + 1), seq.getY(i + 1), keys);
      }
    }
    keys.forEach(key -> {
      ArrayList<T> bin = bins.get(key);
      if (bin != null) {
        result.addAll(bin);
      }
      return true;
    });
    return result;
  }

  /**
   * Collect the bin keys touched by a segment's axis-aligned bounding box into {@code out}.
   * Mirrors the bin-key math in {@link #visit(Envelope, boolean, BinVisitor)} but operates on
   * primitive coordinates so the hot query loop has no per-segment object allocations.
   */
  private void collectBinKeys(double x1, double y1, double x2, double y2, TLongSet out) {
    double minX = clampLon(Math.min(x1, x2));
    double maxX = clampLon(Math.max(x1, x2));
    double minY = clampLat(Math.min(y1, y2));
    double maxY = clampLat(Math.max(y1, y2));
    long minXKey = Math.round(minX / xBinSize);
    long maxXKey = Math.round(maxX / xBinSize);
    long minYKey = Math.round(minY / yBinSize);
    long maxYKey = Math.round(maxY / yBinSize);
    for (long xKey = minXKey; xKey <= maxXKey; xKey++) {
      for (long yKey = minYKey; yKey <= maxYKey; yKey++) {
        out.add(binKey(xKey, yKey));
      }
    }
  }

  /**
   * Pack (xKey, yKey) into a single {@code long} hash key. The xKey halves are swapped so the
   * default long {@code hashCode} ({@code (int) (v ^ (v >>> 32))}) distributes well — see the
   * note in {@link #visit(Envelope, boolean, BinVisitor)}.
   */
  private static long binKey(long xKey, long yKey) {
    return (yKey << 32) | ((xKey & 0xFFFF) << 16) | ((xKey >> 16) & 0xFFFF);
  }

  private static double clampLon(double x) {
    return Math.clamp(x, -180, 180);
  }

  private static double clampLat(double y) {
    return Math.clamp(y, -90, 90);
  }

  /**
   * Make each bin be exactly the required size. This is helpful for large indices, which are mostly
   * used for reads only.
   */
  public void compact() {
    bins.forEachValue(ts -> {
      ts.trimToSize();
      return true;
    });
  }

  public String toString() {
    return String.format(
      Locale.ROOT,
      "HashGridSpatialIndex %f x %f, %d bins allocated, %d objs, %d entries (avg %.2f entries/bin, %.2f entries/object)",
      this.xBinSize,
      this.yBinSize,
      this.nBins,
      this.nObjects,
      this.nEntries,
      (this.nEntries * 1.0) / this.nBins,
      (this.nEntries * 1.0) / this.nObjects
    );
  }

  /** Clamp a coordinate to allowable lat/lon values. */
  private static Coordinate clamp(Coordinate coord) {
    if (Math.abs(coord.x) > 180 || Math.abs(coord.y) > 90) {
      LOG.warn(
        "Corner of envelope {} was invalid, clamping to valid range. Perhaps you're buffering something near a pole?",
        coord
      );
      return new Coordinate(clampLon(coord.x), clampLat(coord.y));
    }
    return coord;
  }

  /**
   * Visit each bin touching the envelope.
   *
   * @param envelope      Self-descripting.
   * @param createIfEmpty Create a new bin if not existing.
   * @param binVisitor    The callback to call for each visited bin.
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
        long mapKey = binKey(xKey, yKey);
        ArrayList<T> bin = bins.get(mapKey);
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

  private interface BinVisitor<T> {
    /**
     * Bin visitor callback.
     *
     * @return true if something has been removed from the bin.
     */
    boolean visit(List<T> bin, long mapKey);
  }
}
