package org.opentripplanner.gtfs.mapping;

import java.util.Arrays;
import java.util.Iterator;
import org.opentripplanner.model.ShapePoint;

/**
 * A representation that stores GTFS shape points in a memory-efficient way and allows you to
 * iterate over them for further processing.
 * <p>
 * It is a compromise between efficient memory consumption and efficient use of CPU by not compressing
 * or encoding the shape but storing the raw coordinate values.
 * <p>
 * It is package-private but implements Iterable, so you should use that as the return type of the
 * mapping process.
 */
class CompactShape implements Iterable<ShapePoint> {

  private static final double NO_VALUE = -9999;
  private static final int INCREASE = 50;
  private double[] lats;
  private double[] lons;
  // we only initialize this if we need it
  private double[] distTraveleds;
  private int maxSequence = (int) NO_VALUE;

  public CompactShape() {
    this.lats = new double[INCREASE];
    this.lons = new double[INCREASE];
    Arrays.fill(this.lats, NO_VALUE);
    Arrays.fill(this.lons, NO_VALUE);
    // distTraveleds is created on demand if required
  }

  public void addPoint(org.onebusaway.gtfs.model.ShapePoint shapePoint) {
    int index = shapePoint.getSequence();
    ensureLatLonCapacity(index);
    lats[index] = shapePoint.getLat();
    lons[index] = shapePoint.getLon();
    if (shapePoint.isDistTraveledSet()) {
      ensureDistTraveledCapacity(index);
      distTraveleds[index] = shapePoint.getDistTraveled();
    }
    if (maxSequence < index) {
      maxSequence = index;
    }
  }

  @Override
  public Iterator<ShapePoint> iterator() {
    return new Iterator<>() {
      private int index = 0;

      @Override
      public boolean hasNext() {
        return index <= maxSequence;
      }

      @Override
      public ShapePoint next() {
        var lat = lats[index];
        while (lat == NO_VALUE) {
          index++;
          lat = lats[index];
        }
        var lon = lons[index];
        Double distTraveled = null;
        if (distTraveleds != null && index - 1 < distTraveleds.length) {
          distTraveled = distTraveleds[index];
          if (distTraveled == NO_VALUE) {
            distTraveled = null;
          }
        }
        var ret = new ShapePoint(index, lat, lon, distTraveled);
        index++;
        return ret;
      }
    };
  }

  private void ensureLatLonCapacity(int index) {
    if (lats.length - 1 < index) {
      int oldLength = lats.length;
      int newLength = increaseCapacity(index);
      lats = Arrays.copyOf(lats, newLength);
      lons = Arrays.copyOf(lons, newLength);
      for (var i = oldLength; i < newLength; i++) {
        lats[i] = NO_VALUE;
        lons[i] = NO_VALUE;
      }
    }
  }

  private void ensureDistTraveledCapacity(int index) {
    if (this.distTraveleds == null) {
      this.distTraveleds = new double[Math.max(INCREASE, index + 1)];
      Arrays.fill(distTraveleds, NO_VALUE);
    } else if (distTraveleds.length - 1 < index) {
      int oldLength = distTraveleds.length;
      int newLength = increaseCapacity(index);
      this.distTraveleds = Arrays.copyOf(distTraveleds, newLength);
      for (var i = oldLength; i < newLength; i++) {
        distTraveleds[i] = NO_VALUE;
      }
    }
  }

  private static int increaseCapacity(int index) {
    return index + INCREASE;
  }
}
