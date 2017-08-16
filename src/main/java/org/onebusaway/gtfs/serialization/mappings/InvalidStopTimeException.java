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
package org.onebusaway.gtfs.serialization.mappings;

import org.onebusaway.csv_entities.exceptions.CsvEntityException;
import org.onebusaway.gtfs.model.StopTime;

/**
 * Indicates the an "arrival_time" or "departure_time" value for in the
 * "stop_times.txt" csv file could not be parsed. Recall that the time takes the
 * form "hh:mm:ss" or "h:mm:ss".
 * 
 * @author bdferris
 * @see StopTime#getArrivalTime()
 * @see StopTime#getDepartureTime()
 */
public class InvalidStopTimeException extends CsvEntityException {

  private static final long serialVersionUID = 1L;

  public InvalidStopTimeException(String stopTimeValue) {
    super(StopTime.class, "invalid stop time: " + stopTimeValue);
  }
}
