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
/**
 * 
 */
package org.onebusaway.gtfs.serialization.comparators;

import java.util.Comparator;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

public class ServiceCalendarDateComparator implements
    Comparator<ServiceCalendarDate> {

  @Override
  public int compare(ServiceCalendarDate o1, ServiceCalendarDate o2) {
    AgencyAndId id1 = o1.getServiceId();
    AgencyAndId id2 = o2.getServiceId();
    int c = id1.compareTo(id2);
    if (c != 0)
      return c;
    ServiceDate d1 = o1.getDate();
    ServiceDate d2 = o2.getDate();
    return d1.compareTo(d2);
  }
}