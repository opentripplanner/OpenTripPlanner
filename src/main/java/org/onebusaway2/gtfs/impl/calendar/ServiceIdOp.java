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
package org.onebusaway.gtfs.impl.calendar;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.onebusaway.gtfs.model.calendar.ServiceInterval;

public abstract class ServiceIdOp implements Comparator<Date> {

  public static final ServiceIdOp ARRIVAL_OP = new ArrivalsServiceDateTimeOp();

  public static final ServiceIdOp DEPARTURE_OP = new DeparturesServiceDateTimeOp();

  public static final ServiceIdOp BOTH_OP = new MaxRangeServiceDateTimeOp();

  private boolean _reverse;

  protected ServiceIdOp(boolean reverse) {
    _reverse = reverse;
  }

  public abstract int getFromTime(ServiceInterval interval);

  public abstract int getToTime(ServiceInterval interval);

  public abstract Date getServiceDate(List<Date> data, int index);

  public int compare(Date a, Date b) {
    int rc = a.compareTo(b);
    if (_reverse)
      rc = -rc;
    return rc;
  }

  public Date shiftTime(ServiceInterval interval, Date time) {
    long offset = getFromTime(interval) * 1000;
    return new Date(time.getTime() - offset);
  }

  /**
   * Returns -1 if the service interval comes before the from-to interval.
   * 
   * Returns 0 if the service interval overlaps the from-to interval.
   * 
   * Returns 1 if the service interval comes after the from-to interval
   * 
   * @param interval
   * @param serviceDate
   * @param from
   * @param to
   * @return
   */
  public int compareInterval(ServiceInterval interval, Date serviceDate,
      Date from, Date to) {
    long serviceFrom = serviceDate.getTime() + getFromTime(interval) * 1000;
    long serviceTo = serviceDate.getTime() + getToTime(interval) * 1000;

    if (_reverse) {
      if (serviceTo >= from.getTime())
        return -1;
      if (to.getTime() >= serviceFrom)
        return 1;
      return 0;
    } else {
      if (serviceTo <= from.getTime())
        return -1;
      if (to.getTime() <= serviceFrom)
        return 1;
      return 0;
    }
  }

  private static class ArrivalsServiceDateTimeOp extends ServiceIdOp {

    protected ArrivalsServiceDateTimeOp() {
      super(true);
    }

    @Override
    public int getFromTime(ServiceInterval interval) {
      return interval.getMaxArrival();
    }

    @Override
    public int getToTime(ServiceInterval interval) {
      return interval.getMinArrival();
    }

    @Override
    public Date getServiceDate(List<Date> data, int index) {
      return data.get(data.size() - 1 - index);
    }
  }

  private static class DeparturesServiceDateTimeOp extends ServiceIdOp {

    protected DeparturesServiceDateTimeOp() {
      super(false);
    }

    @Override
    public int getFromTime(ServiceInterval interval) {
      return interval.getMinDeparture();
    }

    @Override
    public int getToTime(ServiceInterval interval) {
      return interval.getMaxDeparture();
    }

    @Override
    public Date getServiceDate(List<Date> data, int index) {
      return data.get(index);
    }
  }

  private static class MaxRangeServiceDateTimeOp extends ServiceIdOp {

    protected MaxRangeServiceDateTimeOp() {
      super(false);
    }

    @Override
    public int getFromTime(ServiceInterval interval) {
      return Math.min(interval.getMinDeparture(), interval.getMinArrival());
    }

    @Override
    public int getToTime(ServiceInterval interval) {
      return Math.max(interval.getMaxDeparture(), interval.getMaxArrival());
    }

    @Override
    public Date getServiceDate(List<Date> data, int index) {
      return data.get(index);
    }
  }

}
