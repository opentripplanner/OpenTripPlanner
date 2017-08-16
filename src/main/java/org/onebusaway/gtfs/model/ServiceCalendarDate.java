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
 * @author bdferris
 * 
 */
@CsvFields(filename = "calendar_dates.txt", required = false)
public final class ServiceCalendarDate extends IdentityBean<Integer> {

  private static final long serialVersionUID = 1L;

  public static final int EXCEPTION_TYPE_ADD = 1;

  public static final int EXCEPTION_TYPE_REMOVE = 2;

  @CsvField(ignore = true)
  private int id;

  @CsvField(mapping = DefaultAgencyIdFieldMappingFactory.class)
  private AgencyAndId serviceId;

  @CsvField(mapping = ServiceDateFieldMappingFactory.class)
  private ServiceDate date;

  private int exceptionType;

  public ServiceCalendarDate() {

  }

  public ServiceCalendarDate(ServiceCalendarDate obj) {
    this.id = obj.id;
    this.serviceId = obj.serviceId;
    this.date = obj.date;
    this.exceptionType = obj.exceptionType;
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

  public ServiceDate getDate() {
    return date;
  }

  public void setDate(ServiceDate date) {
    this.date = date;
  }

  public int getExceptionType() {
    return exceptionType;
  }

  public void setExceptionType(int exceptionType) {
    this.exceptionType = exceptionType;
  }

  @Override
  public String toString() {
    return "<CalendarDate serviceId=" + this.serviceId + " date=" + this.date
        + " exception=" + this.exceptionType + ">";
  }
}
