/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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

import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.csv_entities.schema.annotations.CsvFields;
import org.onebusaway.gtfs.serialization.mappings.EntityFieldMappingFactory;

@CsvFields(filename = "transfers.txt", required = false)
public final class Transfer extends IdentityBean<Integer> {

  private static final long serialVersionUID = 1L;

  private static final int MISSING_VALUE = -999;

  @CsvField(ignore = true)
  private int id;

  @CsvField(name = "from_stop_id", mapping = EntityFieldMappingFactory.class)
  private Stop fromStop;

  @CsvField(name = "from_route_id", mapping = EntityFieldMappingFactory.class, optional = true)
  private Route fromRoute;

  @CsvField(name = "from_trip_id", mapping = EntityFieldMappingFactory.class, optional = true)
  private Trip fromTrip;

  @CsvField(name = "to_stop_id", mapping = EntityFieldMappingFactory.class)
  private Stop toStop;

  @CsvField(name = "to_route_id", mapping = EntityFieldMappingFactory.class, optional = true)
  private Route toRoute;

  @CsvField(name = "to_trip_id", mapping = EntityFieldMappingFactory.class, optional = true)
  private Trip toTrip;

  private int transferType;

  @CsvField(optional = true)
  private int minTransferTime = MISSING_VALUE;

  public Transfer() {

  }

  public Transfer(Transfer obj) {
    this.id = obj.id;
    this.fromStop = obj.fromStop;
    this.fromRoute = obj.fromRoute;
    this.fromTrip = obj.fromTrip;
    this.toStop = obj.toStop;
    this.toRoute = obj.toRoute;
    this.toTrip = obj.toTrip;
    this.transferType = obj.transferType;
    this.minTransferTime = obj.minTransferTime;
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public Stop getFromStop() {
    return fromStop;
  }

  public void setFromStop(Stop fromStop) {
    this.fromStop = fromStop;
  }

  public Route getFromRoute() {
    return fromRoute;
  }

  public void setFromRoute(Route fromRoute) {
    this.fromRoute = fromRoute;
  }

  public Trip getFromTrip() {
    return fromTrip;
  }

  public void setFromTrip(Trip fromTrip) {
    this.fromTrip = fromTrip;
  }

  public Stop getToStop() {
    return toStop;
  }

  public void setToStop(Stop toStop) {
    this.toStop = toStop;
  }

  public Route getToRoute() {
    return toRoute;
  }

  public void setToRoute(Route toRoute) {
    this.toRoute = toRoute;
  }

  public Trip getToTrip() {
    return toTrip;
  }

  public void setToTrip(Trip toTrip) {
    this.toTrip = toTrip;
  }

  public int getTransferType() {
    return transferType;
  }

  public void setTransferType(int transferType) {
    this.transferType = transferType;
  }

  public boolean isMinTransferTimeSet() {
    return minTransferTime != MISSING_VALUE;
  }

  public int getMinTransferTime() {
    return minTransferTime;
  }

  public void setMinTransferTime(int minTransferTime) {
    this.minTransferTime = minTransferTime;
  }

  public void clearMinTransferTime() {
    this.minTransferTime = MISSING_VALUE;
  }

  public String toString() {
    return "<Transfer " + getId() + ">";
  }
}
