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
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.serialization.mappings.DefaultAgencyIdFieldMappingFactory;
import org.onebusaway.gtfs.serialization.mappings.ServiceDateFieldMappingFactory;

/**
 * Note that I decided to call this class ServiceCalendar instead of Calendar,
 * so as to avoid confusion with java.util.Calendar
 * 
 * @author bdferris
 * 
 */
@CsvFields(filename = "calendar.txt", required = false)
public final class ServiceCalendar extends IdentityBean<Integer> {

  private static final long serialVersionUID = 1L;

  @CsvField(ignore = true)
  private int id;

  @CsvField(mapping = DefaultAgencyIdFieldMappingFactory.class)
  private AgencyAndId serviceId;

  private int monday;

  private int tuesday;

  private int wednesday;

  private int thursday;

  private int friday;

  private int saturday;

  private int sunday;

  @CsvField(mapping = ServiceDateFieldMappingFactory.class)
  private ServiceDate startDate;

  @CsvField(mapping = ServiceDateFieldMappingFactory.class)
  private ServiceDate endDate;

  public ServiceCalendar() {

  }

  public ServiceCalendar(ServiceCalendar sc) {
    this.id = sc.id;
    this.serviceId = sc.serviceId;
    this.monday = sc.monday;
    this.tuesday = sc.tuesday;
    this.wednesday = sc.wednesday;
    this.thursday = sc.thursday;
    this.friday = sc.friday;
    this.saturday = sc.saturday;
    this.sunday = sc.sunday;
    this.startDate = sc.startDate;
    this.endDate = sc.endDate;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public AgencyAndId getServiceId() {
    return serviceId;
  }

  public void setServiceId(AgencyAndId serviceId) {
    this.serviceId = serviceId;
  }

  public int getMonday() {
    return monday;
  }

  public void setMonday(int monday) {
    this.monday = monday;
  }

  public int getTuesday() {
    return tuesday;
  }

  public void setTuesday(int tuesday) {
    this.tuesday = tuesday;
  }

  public int getWednesday() {
    return wednesday;
  }

  public void setWednesday(int wednesday) {
    this.wednesday = wednesday;
  }

  public int getThursday() {
    return thursday;
  }

  public void setThursday(int thursday) {
    this.thursday = thursday;
  }

  public int getFriday() {
    return friday;
  }

  public void setFriday(int friday) {
    this.friday = friday;
  }

  public int getSaturday() {
    return saturday;
  }

  public void setSaturday(int saturday) {
    this.saturday = saturday;
  }

  public int getSunday() {
    return sunday;
  }

  public void setSunday(int sunday) {
    this.sunday = sunday;
  }

  public ServiceDate getStartDate() {
    return startDate;
  }

  public void setStartDate(ServiceDate startDate) {
    this.startDate = startDate;
  }

  public ServiceDate getEndDate() {
    return endDate;
  }

  public void setEndDate(ServiceDate endDate) {
    this.endDate = endDate;
  }

  public String toString() {
    return "<ServiceCalendar " + this.serviceId + " [" + this.monday
        + this.tuesday + this.wednesday + this.thursday + this.friday
        + this.saturday + this.sunday + "]>";
  }
}
