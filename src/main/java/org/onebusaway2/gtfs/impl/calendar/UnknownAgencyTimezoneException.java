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

import java.util.TimeZone;

import org.onebusaway.csv_entities.exceptions.CsvEntityException;
import org.onebusaway.gtfs.model.Agency;

/**
 * Indicates that the {@link Agency#getTimezone()} string does not evaluate to a
 * valid {@link TimeZone}.
 * 
 * @author bdferris
 * @see TimeZone#getTimeZone(String)
 */
public class UnknownAgencyTimezoneException extends CsvEntityException {

  private static final long serialVersionUID = 1L;

  public UnknownAgencyTimezoneException(String agencyName, String timezone) {
    super(Agency.class, "unknown timezone \"" + timezone + "\" for agency \""
        + agencyName + "\"");
  }
}
