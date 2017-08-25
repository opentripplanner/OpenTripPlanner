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

import java.io.Serializable;

import org.onebusaway.gtfs.model.calendar.ServiceDate;

/**
 * An identifier class that combines a {@link AgencyAndId} id with a service
 * date. See {@link ServiceDate} for more details of the service date idea.
 * 
 * @author bdferris
 * @see AgencyAndId
 * @see ServiceDate
 */
public class AgencyAndIdInstance implements Serializable,
    Comparable<AgencyAndIdInstance> {

  private static final long serialVersionUID = 1L;

  private final AgencyAndId id;

  private final long serviceDate;

  public AgencyAndIdInstance(AgencyAndId id, long serviceDate) {
    if (id == null)
      throw new IllegalArgumentException("id cannot be null");
    this.id = id;
    this.serviceDate = serviceDate;
  }

  public AgencyAndId getId() {
    return id;
  }

  public long getServiceDate() {
    return serviceDate;
  }

  @Override
  public int compareTo(AgencyAndIdInstance o) {
    int c = this.id.compareTo(o.id);
    if (c == 0)
      c = this.serviceDate == o.serviceDate ? 0
          : (this.serviceDate < o.serviceDate ? -1 : 1);
    return c;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id.hashCode();
    result = prime * result + (int) (serviceDate ^ (serviceDate >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AgencyAndIdInstance other = (AgencyAndIdInstance) obj;
    if (!id.equals(other.id))
      return false;
    if (serviceDate != other.serviceDate)
      return false;
    return true;
  }
}
