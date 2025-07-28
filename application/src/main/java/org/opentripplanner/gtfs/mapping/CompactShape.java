package org.opentripplanner.gtfs.mapping;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;
import org.opentripplanner.model.ShapePoint;

/**
 * A representation that stores GTFS shape points in a memory-efficient way and allows you to
 * iterate over them for further processing.
 * <p>
 * Shape points are stored in an automatically expanding sparse array of original coordinate values,
 * so when they are added out of order, the shape doesn't need to be encoded or sorted again.
 * It is a compromise between efficient memory consumption and efficient use of CPU by not compressing
 * or encoding the points.
 * <p>
 * It is package-private but implements Iterable, so you should use that as the return type of the
 * mapping process.
 */
class CompactShape implements Iterable<ShapePoint> {

  private static final double NO_VALUE = -9999;
  private static final int CAPACITY = 50;
  private final TDoubleList lats = new TDoubleArrayList(CAPACITY, NO_VALUE);
  private final TDoubleList lons = new TDoubleArrayList(CAPACITY, NO_VALUE);
  private final TDoubleList distTraveleds = new TDoubleArrayList(CAPACITY, NO_VALUE);
  /**
   * The mapping from GTFS sequence number to index in the array lists. This allows the
   * shape points to be inserted in any order and the sequence number to have very large
   * gaps - at the cost of consuming a bit more memory.
   */
  private final TIntIntMap seqIndex = new TIntIntHashMap();

  CompactShape() {}

  public void addPoint(org.onebusaway.gtfs.model.ShapePoint shapePoint) {
    lats.add(shapePoint.getLat());
    lons.add(shapePoint.getLon());
    if (shapePoint.isDistTraveledSet()) {
      distTraveleds.add(shapePoint.getDistTraveled());
    } else {
      distTraveleds.add(NO_VALUE);
    }

    int seq = shapePoint.getSequence();
    seqIndex.put(seq, lats.size() - 1);
  }

  @Override
  public Iterator<ShapePoint> iterator() {
    return new Iterator<>() {
      private final PrimitiveIterator.OfInt seqIterator = IntStream.of(seqIndex.keys())
        .sorted()
        .iterator();

      @Override
      public boolean hasNext() {
        return seqIterator.hasNext();
      }

      @Override
      public ShapePoint next() {
        var seq = seqIterator.nextInt();
        var index = seqIndex.get(seq);
        var lat = lats.get(index);
        var lon = lons.get(index);
        Double distTraveled = distTraveleds.get(index);
        if (distTraveled == NO_VALUE) {
          distTraveled = null;
        }
        return new ShapePoint(seq, lat, lon, distTraveled);
      }
    };
  }
}
