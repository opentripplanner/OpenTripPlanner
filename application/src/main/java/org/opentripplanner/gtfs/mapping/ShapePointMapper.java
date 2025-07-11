package org.opentripplanner.gtfs.mapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/** Responsible for mapping GTFS ShapePoint into the OTP model. */
class ShapePointMapper {

  private final IdFactory idFactory;

  ShapePointMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  Map<FeedScopedId, CompactShapeBuilder> map(
    Collection<org.onebusaway.gtfs.model.ShapePoint> allShapePoints
  ) {
    var ret = new HashMap<FeedScopedId, CompactShapeBuilder>();
    for (var shapePoint : allShapePoints) {
      var shapeId = idFactory.createId(shapePoint.getShapeId(), "shape point");
      var shapeBuilder = ret.getOrDefault(shapeId, new CompactShapeBuilder());
      shapeBuilder.addPoint(shapePoint);
      ret.put(shapeId, shapeBuilder);
    }

    return ret;
  }
}

class CompactShapeBuilder {

  private static final double NO_VALUE = -9999;
  private static final int INCREASE = 50;
  private double[] lats;
  private double[] lons;
  // we only initialize this if we need it
  private double[] distTraveleds;
  private int maxSequence = (int) NO_VALUE;

  public CompactShapeBuilder() {
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
    if (index > maxSequence) {
      maxSequence = index;
    }
  }

  public Iterable<ShapePoint> shapePoints() {
    return () ->
      new Iterator<>() {
        private int index = 0;

        @Override
        public boolean hasNext() {
          return index < maxSequence;
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
          if (distTraveleds != null) {
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
    if (lats.length < index + 1) {
      int oldLength = lats.length;
      int newLength = increaseCapacity(lats);
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
      this.distTraveleds = new double[INCREASE];
      Arrays.fill(distTraveleds, NO_VALUE);
    } else if (distTraveleds.length < index + 1) {
      int newLength = increaseCapacity(distTraveleds);
      this.distTraveleds = Arrays.copyOf(distTraveleds, newLength);
    }
  }

  private static int increaseCapacity(double[] array) {
    return Math.max(INCREASE, array.length + INCREASE);
  }
}
