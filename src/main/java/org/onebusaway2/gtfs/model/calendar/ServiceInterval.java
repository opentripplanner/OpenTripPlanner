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

/**
 * Specifies an immutable interval of min and max arrival and departure times.
 * 
 * @author bdferris
 * 
 */
public final class ServiceInterval implements Serializable {

  private static final long serialVersionUID = 1L;

  private final int minArrival;
  private final int minDeparture;
  private final int maxArrival;
  private final int maxDeparture;

  /**
   * 
   * @param arrival arrival time in seconds since midnight
   * @param departure departure time in seconds since midnight
   */
  public ServiceInterval(int arrival, int departure) {
    this(arrival, departure, arrival, departure);
  }

  /**
   * 
   * @param minArrival min arrival time in seconds since midnight
   * @param minDeparture min departure time in seconds since midnight
   * @param maxArrival max arrival time in seconds since midnight
   * @param maxDeparture max departue time in seconds since midnight
   */
  public ServiceInterval(int minArrival, int minDeparture, int maxArrival,
      int maxDeparture) {
    this.minArrival = Math.min(minArrival, maxArrival);
    this.minDeparture = Math.min(minDeparture, maxDeparture);
    this.maxArrival = Math.max(minArrival, maxArrival);
    this.maxDeparture = Math.max(minDeparture, maxDeparture);
  }

  /**
   * 
   * @return min arrival time in seconds since midnight
   */
  public int getMinArrival() {
    return minArrival;
  }

  /**
   * 
   * @return min departure time in seconds since midnight
   */
  public int getMinDeparture() {
    return minDeparture;
  }

  /**
   * 
   * @return max arrival time in seconds since midnight
   */
  public int getMaxArrival() {
    return maxArrival;
  }

  /**
   * 
   * @return max departure time in seconds since midnight
   */
  public int getMaxDeparture() {
    return maxDeparture;
  }

  /**
   * Construct a new {@link ServiceInterval} by extending the current service
   * interval, adjusting the arrival and departure intervals to include the
   * additional arrival and departure time specified in the arguments.
   * 
   * @param arrivalTime a new arrival time to incorporate in the extended
   *          interval
   * @param departureTime a new departure time to incorporate in the extended
   *          interval
   * @return a new interval with the additional arrival and departure times
   *         incorporated
   */
  public ServiceInterval extend(int arrivalTime, int departureTime) {
    int minArrivalTime = Math.min(minArrival, arrivalTime);
    int minDepartureTime = Math.min(minDeparture, departureTime);
    int maxArrivalTime = Math.max(maxArrival, arrivalTime);
    int maxDepartureTime = Math.max(maxDeparture, departureTime);
    return new ServiceInterval(minArrivalTime, minDepartureTime,
        maxArrivalTime, maxDepartureTime);
  }

  public static ServiceInterval extend(ServiceInterval serviceInterval,
      int arrivalTime, int departureTime) {
    if (serviceInterval == null)
      return new ServiceInterval(arrivalTime, departureTime);
    else
      return serviceInterval.extend(arrivalTime, departureTime);
  }

  @Override
  public String toString() {
    return "Interval(min=" + minArrival + "," + minDeparture + " max="
        + maxArrival + "," + maxDeparture + ")";
  }
}
