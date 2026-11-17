package org.opentripplanner.gtfs.mapping;

import com.carrotsearch.hppc.DoubleArrayList;
import com.carrotsearch.hppc.IntArrayList;
import java.util.Iterator;
import java.util.stream.IntStream;
import org.opentripplanner.model.ShapePoint;

/**
 * A representation that stores GTFS shape points in a memory-efficient way and allows you to
 * iterate over them for further processing.
 * <p>
 * The fields of ShapePoints are stored densely in automatically expanding HPPC primitive lists.
 * When later needed, they are reconstituted into objects and sorted on their sequence number. Only
 * an iterator over the sorted collection escapes rather than the collection itself, providing some
 * assurance that the objects will be quickly garbage collected.
 * <p>
 * This class is package-private but implements Iterable, so you should use that as the return
 * type of the mapping process.
 */
class CompactShape implements Iterable<ShapePoint> {

  private static final int INITIAL_CAPACITY = 50;
  private static final double NO_DISTANCE = -9999;

  private final IntArrayList seqs = new IntArrayList(INITIAL_CAPACITY);
  private final DoubleArrayList lats = new DoubleArrayList(INITIAL_CAPACITY);
  private final DoubleArrayList lons = new DoubleArrayList(INITIAL_CAPACITY);
  private final DoubleArrayList dists = new DoubleArrayList(INITIAL_CAPACITY);

  public void addPoint(org.onebusaway.gtfs.model.ShapePoint shapePoint) {
    seqs.add(shapePoint.getSequence());
    lats.add(shapePoint.getLat());
    lons.add(shapePoint.getLon());
    dists.add(shapePoint.isDistTraveledSet() ? shapePoint.getDistTraveled() : NO_DISTANCE);
  }

  @Override
  public Iterator<ShapePoint> iterator() {
    return IntStream.range(0, lats.size())
      .mapToObj(i -> {
        double dist = dists.get(i);
        return new ShapePoint(seqs.get(i), lats.get(i), lons.get(i), dist < 0 ? null : dist);
      })
      .sorted()
      .iterator();
  }
}
