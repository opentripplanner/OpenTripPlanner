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
package org.onebusaway.gtfs.model;

/**
 * 
 * @author bdferris
 * 
 * @see ShapePoint#setProxy(ShapePointProxy)
 */
public interface ShapePointProxy {

  public Integer getId();

  public void setId(Integer id);

  public AgencyAndId getShapeId();

  public void setShapeId(AgencyAndId shapeId);

  public int getSequence();

  public void setSequence(int sequence);

  public boolean isDistTraveledSet();

  public double getDistTraveled();

  public void setDistTraveled(double distTraveled);

  public void clearDistTraveled();

  public double getLat();

  public void setLat(double lat);

  public double getLon();

  public void setLon(double lon);
}
