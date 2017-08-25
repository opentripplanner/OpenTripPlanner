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
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;

/**
 * The GTFS spec declares that at least one of {@link Route#getShortName()} or
 * {@link Route#getLongName()} must be specified, if not both. If neither is set
 * for a route in a feed, this exception is thrown.
 * 
 * @author bdferris
 */
public class RouteNameException extends CsvEntityException {

  private static final long serialVersionUID = 1L;

  public RouteNameException(AgencyAndId routeId) {
    super(Route.class,"either shortName or longName must be set for route=" + routeId);
  }
}
