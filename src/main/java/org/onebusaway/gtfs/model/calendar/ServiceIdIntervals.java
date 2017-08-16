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
package org.onebusaway.gtfs.model.calendar;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A {@link Map} of {@link LocalizedServiceId} and {@link ServiceInterval}
 * objects, with convenience methods for adding additional service ids and
 * arrival-departure time intervals.
 * 
 * @author bdferris
 * 
 */
public class ServiceIdIntervals implements Serializable,
    Iterable<Map.Entry<LocalizedServiceId, ServiceInterval>> {

  private static final long serialVersionUID = 1L;

  private Map<LocalizedServiceId, ServiceInterval> _intervals = new HashMap<LocalizedServiceId, ServiceInterval>();

  public void addStopTime(LocalizedServiceId serviceId, int arrivalTime,
      int departureTime) {

    ServiceInterval interval = _intervals.get(serviceId);

    if (interval == null)
      interval = new ServiceInterval(arrivalTime, departureTime);
    else
      interval = interval.extend(arrivalTime, departureTime);

    _intervals.put(serviceId, interval);
  }

  public void addIntervals(ServiceIdIntervals intervals) {
    for (Map.Entry<LocalizedServiceId, ServiceInterval> entry : intervals) {
      LocalizedServiceId serviceId = entry.getKey();
      ServiceInterval interval = entry.getValue();
      addStopTime(serviceId, interval.getMinArrival(),
          interval.getMinDeparture());
      addStopTime(serviceId, interval.getMaxArrival(),
          interval.getMaxDeparture());
    }
  }

  public Set<LocalizedServiceId> getServiceIds() {
    return _intervals.keySet();
  }

  public ServiceInterval getIntervalForServiceId(LocalizedServiceId serviceId) {
    return _intervals.get(serviceId);
  }

  @Override
  public Iterator<Entry<LocalizedServiceId, ServiceInterval>> iterator() {
    return _intervals.entrySet().iterator();
  }

}
