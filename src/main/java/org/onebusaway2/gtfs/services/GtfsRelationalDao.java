/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2011 Google, Inc.
 * Copyright (C) 2011 Laurent Gregoire <laurent.gregoire@gmail.com>
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
package org.onebusaway.gtfs.services;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.FareRule;
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

import java.util.List;

/**
 * While {@link GtfsDao} has basic methods for retrieving collections of
 * entities and entities by id, {@link GtfsRelationalDao} adds some basic
 * methods for retrieving entities using more complex data relations.
 * 
 * You can imagine many complex queries that you might perform on GTFS data,
 * most of which will not be included here. These are just some basic relational
 * methods that we use to bootstrap other GTFS classes. To add more complex
 * queries, look at the specific mechanisms provided by classes implementing
 * {@link GtfsRelationalDao} and {@link GtfsDao}.
 * 
 * @author bdferris
 */
public interface GtfsRelationalDao extends GtfsDao {

  /****
   * ServiceId Methods
   ****/

  public List<String> getTripAgencyIdsReferencingServiceId(AgencyAndId serviceId);

  /****
   * Route Methods
   ****/

  public List<Route> getRoutesForAgency(Agency agency);

  /****
   * Stop Methods
   ****/

  public List<Stop> getStopsForStation(Stop station);

  /****
   * {@link Trip} Methods
   ****/

  public List<Trip> getTripsForRoute(Route route);

  public List<Trip> getTripsForShapeId(AgencyAndId shapeId);

  public List<Trip> getTripsForServiceId(AgencyAndId serviceId);

  public List<Trip> getTripsForBlockId(AgencyAndId blockId);

  /****
   * {@link StopTime} Methods
   ****/

  /**
   * @return the list of {@link StopTime} objects associated with the trip,
   *         sorted by {@link StopTime#getStopSequence()}
   */
  public List<StopTime> getStopTimesForTrip(Trip trip);

  /**
   * @return the list of {@link StopTime} objects associated with the specified
   *         {@link Stop}, in no particular order
   */
  public List<StopTime> getStopTimesForStop(Stop stop);

  /****
   * {@link ShapePoint} Methods
   ****/

  public List<AgencyAndId> getAllShapeIds();

  public List<ShapePoint> getShapePointsForShapeId(AgencyAndId shapeId);

  /****
   * {@link Frequency} Methods
   ****/

  public List<Frequency> getFrequenciesForTrip(Trip trip);

  /****
   * {@link ServiceCalendar} Methods
   ****/

  public List<AgencyAndId> getAllServiceIds();

  public ServiceCalendar getCalendarForServiceId(AgencyAndId serviceId);

  /****
   * {@link ServiceCalendarDate} Methods
   ****/

  public List<ServiceCalendarDate> getCalendarDatesForServiceId(
      AgencyAndId serviceId);

  /****
   * {@link FareRule}
   *****/

  public List<FareRule> getFareRulesForFareAttribute(FareAttribute fareAttribute);

}
