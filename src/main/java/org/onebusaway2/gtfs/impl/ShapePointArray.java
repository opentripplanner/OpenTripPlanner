/**
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.gtfs.impl;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.ShapePointProxy;

public class ShapePointArray extends AbstractList<ShapePoint> {

  private int size = 0;

  private AgencyAndId[] shapeIds = new AgencyAndId[0];

  private int[] sequences = new int[0];

  private double[] lats = new double[0];

  private double[] lons = new double[0];

  private double[] distTraveled = new double[0];

  public void trimToSize() {
    setLength(size);
  }

  /****
   * {@link List} Interface
   ****/

  @Override
  public boolean add(ShapePoint shapePoint) {
    int index = size;
    size++;
    ensureCapacity(size);
    shapeIds[index] = shapePoint.getShapeId();
    sequences[index] = shapePoint.getSequence();
    lats[index] = shapePoint.getLat();
    lons[index] = shapePoint.getLon();
    distTraveled[index] = shapePoint.getDistTraveled();
    return true;
  }

  @Override
  public void clear() {
    size = 0;
    setLength(0);
  }

  @Override
  public Iterator<ShapePoint> iterator() {
    return new ShapePointIterator();
  }

  @Override
  public ShapePoint get(int index) {
    if (index < 0 || index >= size) {
      throw new NoSuchElementException();
    }
    ShapePoint shapePoint = new ShapePoint();
    shapePoint.setProxy(new ShapePointProxyImpl(index));
    return shapePoint;
  }

  @Override
  public int size() {
    return size;
  }

  /****
   * Private Methods
   ****/

  private void ensureCapacity(int capacity) {
    if (shapeIds.length < capacity) {
      int newLength = Math.max(8, shapeIds.length << 2);
      setLength(newLength);
    }
  }

  private void setLength(int newLength) {
    this.shapeIds = Arrays.copyOf(this.shapeIds, newLength);
    this.sequences = Arrays.copyOf(this.sequences, newLength);
    this.lats = Arrays.copyOf(this.lats, newLength);
    this.lons = Arrays.copyOf(this.lons, newLength);
    this.distTraveled = Arrays.copyOf(this.distTraveled, newLength);
  }

  private class ShapePointIterator implements Iterator<ShapePoint> {

    private int index = 0;

    @Override
    public boolean hasNext() {
      return index < size;
    }

    @Override
    public ShapePoint next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      ShapePoint shapePoint = new ShapePoint();
      shapePoint.setProxy(new ShapePointProxyImpl(index));
      index++;
      return shapePoint;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private class ShapePointProxyImpl implements ShapePointProxy {

    private final int index;

    public ShapePointProxyImpl(int index) {
      this.index = index;
    }

    @Override
    public Integer getId() {
      return index;
    }

    @Override
    public void setId(Integer id) {
      // ignored
    }

    @Override
    public AgencyAndId getShapeId() {
      return shapeIds[index];
    }

    @Override
    public void setShapeId(AgencyAndId shapeId) {
      shapeIds[index] = shapeId;
    }

    @Override
    public int getSequence() {
      return sequences[index];
    }

    @Override
    public void setSequence(int sequence) {
      sequences[index] = sequence;
    }

    @Override
    public boolean isDistTraveledSet() {
      return distTraveled[index] != ShapePoint.MISSING_VALUE;
    }

    @Override
    public double getDistTraveled() {
      return distTraveled[index];
    }

    @Override
    public void setDistTraveled(double distTraveled) {
      ShapePointArray.this.distTraveled[index] = distTraveled;
    }

    @Override
    public void clearDistTraveled() {
      distTraveled[index] = ShapePoint.MISSING_VALUE;
    }

    @Override
    public double getLat() {
      return lats[index];
    }

    @Override
    public void setLat(double lat) {
      lats[index] = lat;
    }

    @Override
    public double getLon() {
      return lons[index];
    }

    @Override
    public void setLon(double lon) {
      lons[index] = lon;
    }
  }

}
