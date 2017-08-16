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
import org.onebusaway.gtfs.serialization.mappings.EntityFieldMappingFactory;

@CsvFields(filename = "pathways.txt", required = false)
public final class Pathway extends IdentityBean<AgencyAndId> {

  private static final long serialVersionUID = -2404871423254094109L;

  private static final int MISSING_VALUE = -999;

  @CsvField(name = "pathway_id", mapping = DefaultAgencyIdFieldMappingFactory.class)
  private AgencyAndId id;

  private int pathwayType;

  @CsvField(name = "from_stop_id", mapping = EntityFieldMappingFactory.class)
  private Stop fromStop;

  @CsvField(name = "to_stop_id", mapping = EntityFieldMappingFactory.class)
  private Stop toStop;

  private int traversalTime;

  @CsvField(optional = true)
  private int wheelchairTraversalTime = MISSING_VALUE;

  @Override
  public AgencyAndId getId() {
    return id;
  }

  @Override
  public void setId(AgencyAndId id) {
    this.id = id;
  }

  public void setPathwayType(int pathwayType) {
    this.pathwayType = pathwayType;
  }

  public int getPathwayType() {
    return pathwayType;
  }
  
  public void setFromStop(Stop fromStop) {
    this.fromStop = fromStop;
  }

  public Stop getFromStop() {
    return fromStop;
  }

  public void setToStop(Stop toStop) {
    this.toStop = toStop;
  }

  public Stop getToStop() {
    return toStop;
  }

  public void setTraversalTime(int traversalTime) {
    this.traversalTime = traversalTime;
  }

  public int getTraversalTime() {
    return traversalTime;
  }

  public void setWheelchairTraversalTime(int wheelchairTraversalTime) {
    this.wheelchairTraversalTime = wheelchairTraversalTime;
  }

  public int getWheelchairTraversalTime() {
    return wheelchairTraversalTime;
  }

  public boolean isWheelchairTraversalTimeSet() {
    return wheelchairTraversalTime != MISSING_VALUE;
  }

  public void clearWheelchairTraversalTime() {
    this.wheelchairTraversalTime = MISSING_VALUE;
  }

  @Override
  public String toString() {
    return "<Pathway " + this.id + ">";
  }
}
