/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.gtfs.model;

import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.csv_entities.schema.annotations.CsvFields;
import org.onebusaway.gtfs.serialization.mappings.DefaultAgencyIdFieldMappingFactory;
import org.onebusaway.gtfs.serialization.mappings.LatLonFieldMappingFactory;

@CsvFields(filename = "shapes.txt", required = false)
public final class ShapePoint extends IdentityBean<Integer> implements
    Comparable<ShapePoint> {

  private static final long serialVersionUID = 1L;

  public static final double MISSING_VALUE = -999;

  @CsvField(ignore = true)
  private int id;

  @CsvField(mapping = DefaultAgencyIdFieldMappingFactory.class)
  private AgencyAndId shapeId;

  @CsvField(name = "shape_pt_sequence")
  private int sequence;

  @CsvField(name = "shape_pt_lat", mapping = LatLonFieldMappingFactory.class)
  private double lat;

  @CsvField(name = "shape_pt_lon", mapping = LatLonFieldMappingFactory.class)
  private double lon;

  @CsvField(optional = true, name = "shape_dist_traveled")
  private double distTraveled = MISSING_VALUE;

  @CsvField(ignore = true)
  private transient ShapePointProxy proxy;

  public ShapePoint() {

  }

  public ShapePoint(ShapePoint shapePoint) {
    this.id = shapePoint.id;
    this.shapeId = shapePoint.shapeId;
    this.sequence = shapePoint.sequence;
    this.distTraveled = shapePoint.distTraveled;
    this.lat = shapePoint.lat;
    this.lon = shapePoint.lon;
  }

  public Integer getId() {
    if (proxy != null) {
      return proxy.getId();
    }
    return id;
  }

  public void setId(Integer id) {
    if (proxy != null) {
      proxy.setId(id);
      return;
    }
    this.id = id;
  }

  public AgencyAndId getShapeId() {
    if (proxy != null) {
      return proxy.getShapeId();
    }
    return shapeId;
  }

  public void setShapeId(AgencyAndId shapeId) {
    if (proxy != null) {
      proxy.setShapeId(shapeId);
      return;
    }
    this.shapeId = shapeId;
  }

  public int getSequence() {
    if (proxy != null) {
      return proxy.getSequence();
    }
    return sequence;
  }

  public void setSequence(int sequence) {
    if (proxy != null) {
      proxy.setSequence(sequence);
      return;
    }
    this.sequence = sequence;
  }

  public boolean isDistTraveledSet() {
    if (proxy != null) {
      return proxy.isDistTraveledSet();
    }
    return distTraveled != MISSING_VALUE;
  }

  /**
   * @return the distance traveled along the shape path. If no distance was
   *         specified, the value is undefined. Check first with
   *         {@link #isDistTraveledSet()}
   */
  public double getDistTraveled() {
    if (proxy != null) {
      return proxy.getDistTraveled();
    }
    return distTraveled;
  }

  public void setDistTraveled(double distTraveled) {
    if (proxy != null) {
      proxy.setDistTraveled(distTraveled);
      return;
    }
    this.distTraveled = distTraveled;
  }

  public void clearDistTraveled() {
    if (proxy != null) {
      proxy.clearDistTraveled();
      return;
    }
    this.distTraveled = MISSING_VALUE;
  }

  public double getLat() {
    if (proxy != null) {
      return proxy.getLat();
    }
    return lat;
  }

  public void setLat(double lat) {
    if (proxy != null) {
      proxy.setLat(lat);
      return;
    }
    this.lat = lat;
  }

  public double getLon() {
    if (proxy != null) {
      return proxy.getLon();
    }
    return lon;
  }

  public void setLon(double lon) {
    if (proxy != null) {
      proxy.setLon(lon);
      return;
    }
    this.lon = lon;
  }

  /**
   * When set, all interactions with the shape point will be redirected through
   * this proxy.
   * 
   * @param proxy
   */
  public void setProxy(ShapePointProxy proxy) {
    this.proxy = proxy;
  }

  public ShapePointProxy getProxy() {
    return proxy;
  }

  @Override
  public String toString() {
    return "<ShapePoint " + getShapeId() + " #" + getSequence() + " ("
        + getLat() + "," + getLon() + ")>";
  }

  @Override
  public int compareTo(ShapePoint o) {
    return this.getSequence() - o.getSequence();
  }
}
