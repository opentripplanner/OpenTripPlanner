/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2012 Google, Inc.
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
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.serialization.GtfsReaderContext;

/**
 * Error indicating that there was a problem finding the appropriate
 * {@link Agency} instance to set in a call to {@link Route#setAgency(Agency)}.
 * When parsing a route from csv, we look at the "agency_id" field first. If it
 * isn't empty, we look for an agency with the specified id. This exception is
 * throw if an agency with the specified id could not be found.
 * 
 * If no "agency_id" field is specified in the csv, we next use the id specified
 * by {@link GtfsReaderContext#getDefaultAgencyId()}. If no default agencyId is
 * specified, or an agency with the specified id cannot be found, we throw this
 * exception.
 * 
 * @author bdferris
 * 
 */
public class AgencyNotFoundForRouteException extends CsvEntityException {

  private static final long serialVersionUID = 1L;

  private Route _route;

  private String _agencyId;

  public AgencyNotFoundForRouteException(Class<?> entityType, Route route,
      String agencyId) {
    super(entityType, "could not find Agency with specified id=" + agencyId
        + " for route " + route);
    _route = route;
    _agencyId = agencyId;
  }

  public AgencyNotFoundForRouteException(Class<?> entityType, Route route) {
    super(entityType, "could not find Agency for route " + route);
    _route = route;
  }

  public Route getRoute() {
    return _route;
  }

  public String getAgencyId() {
    return _agencyId;
  }

}
