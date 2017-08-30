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
package org.onebusaway2.gtfs.impl;

import java.util.Collection;

import org.onebusaway2.gtfs.model.Agency;
import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.FareAttribute;
import org.onebusaway2.gtfs.model.FareRule;
import org.onebusaway2.gtfs.model.FeedInfo;
import org.onebusaway2.gtfs.model.Frequency;
import org.onebusaway2.gtfs.model.Pathway;
import org.onebusaway2.gtfs.model.Route;
import org.onebusaway2.gtfs.model.ServiceCalendar;
import org.onebusaway2.gtfs.model.ServiceCalendarDate;
import org.onebusaway2.gtfs.model.ShapePoint;
import org.onebusaway2.gtfs.model.Stop;
import org.onebusaway2.gtfs.model.StopTime;
import org.onebusaway2.gtfs.model.Transfer;
import org.onebusaway2.gtfs.model.Trip;
import org.onebusaway2.gtfs.services.GtfsDao;
import org.onebusaway2.gtfs.services.GtfsMutableDao;

public class GtfsDaoImpl extends GenericDaoImpl implements GtfsMutableDao {

  /***
   * {@link GtfsDao} Interface
   ****/

  public Agency getAgencyForId(String id) {
    return getEntityForId(Agency.class, id);
  }

  public Collection<Agency> getAllAgencies() {
    return getAllEntitiesForType(Agency.class);
  }

  public Collection<ServiceCalendarDate> getAllCalendarDates() {
    return getAllEntitiesForType(ServiceCalendarDate.class);
  }

  public Collection<ServiceCalendar> getAllCalendars() {
    return getAllEntitiesForType(ServiceCalendar.class);
  }

  public Collection<FareAttribute> getAllFareAttributes() {
    return getAllEntitiesForType(FareAttribute.class);
  }

  public Collection<FareRule> getAllFareRules() {
    return getAllEntitiesForType(FareRule.class);
  }

  @Override
  public Collection<FeedInfo> getAllFeedInfos() {
    return getAllEntitiesForType(FeedInfo.class);
  }

  public Collection<Frequency> getAllFrequencies() {
    return getAllEntitiesForType(Frequency.class);
  }

  public Collection<Route> getAllRoutes() {
    return getAllEntitiesForType(Route.class);
  }

  public Collection<ShapePoint> getAllShapePoints() {
    return getAllEntitiesForType(ShapePoint.class);
  }

  public Collection<StopTime> getAllStopTimes() {
    return super.getAllEntitiesForType(StopTime.class);
  }

  public Collection<Stop> getAllStops() {
    return getAllEntitiesForType(Stop.class);
  }

  public Collection<Transfer> getAllTransfers() {
    return getAllEntitiesForType(Transfer.class);
  }

  public Collection<Trip> getAllTrips() {
    return getAllEntitiesForType(Trip.class);
  }

  public ServiceCalendarDate getCalendarDateForId(int id) {
    return getEntityForId(ServiceCalendarDate.class, id);
  }

  public ServiceCalendar getCalendarForId(int id) {
    return getEntityForId(ServiceCalendar.class, id);
  }

  public FareAttribute getFareAttributeForId(AgencyAndId id) {
    return getEntityForId(FareAttribute.class, id);
  }

  public FareRule getFareRuleForId(int id) {
    return getEntityForId(FareRule.class, id);
  }

  @Override
  public FeedInfo getFeedInfoForId(int id) {
    return getEntityForId(FeedInfo.class, id);
  }

  public Frequency getFrequencyForId(int id) {
    return getEntityForId(Frequency.class, id);
  }

  public Collection<Pathway> getAllPathways() {
    return getAllEntitiesForType(Pathway.class);
  }

  public Pathway getPathwayForId(AgencyAndId id) {
    return getEntityForId(Pathway.class, id);
  }

  public Route getRouteForId(AgencyAndId id) {
    return getEntityForId(Route.class, id);
  }

  public ShapePoint getShapePointForId(int id) {
    return getEntityForId(ShapePoint.class, id);
  }

  public Stop getStopForId(AgencyAndId id) {
    return getEntityForId(Stop.class, id);
  }

  public StopTime getStopTimeForId(int id) {
    return getEntityForId(StopTime.class, id);
  }

  public Transfer getTransferForId(int id) {
    return getEntityForId(Transfer.class, id);
  }

  public Trip getTripForId(AgencyAndId id) {
    return getEntityForId(Trip.class, id);
  }
}
