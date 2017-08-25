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
package org.onebusaway.gtfs.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.FareRule;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.model.Pathway;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.gtfs.services.GtfsDao;
import org.onebusaway.gtfs.services.GtfsMutableDao;

public class GtfsDaoImpl extends GenericDaoImpl implements GtfsMutableDao {

  private StopTimeArray stopTimes = new StopTimeArray();

  private ShapePointArray shapePoints = new ShapePointArray();

  private boolean packStopTimes = false;

  private boolean packShapePoints = false;

  public boolean isPackStopTimes() {
    return packStopTimes;
  }

  public void setPackStopTimes(boolean packStopTimes) {
    this.packStopTimes = packStopTimes;
  }

  public boolean isPackShapePoints() {
    return packShapePoints;
  }

  public void setPackShapePoints(boolean packShapePoints) {
    this.packShapePoints = packShapePoints;
  }

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
    if (packShapePoints) {
      return shapePoints;
    }
    return getAllEntitiesForType(ShapePoint.class);
  }

  public Collection<StopTime> getAllStopTimes() {
    if (packStopTimes) {
      return stopTimes;
    }
    return getAllEntitiesForType(StopTime.class);
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
    if (packShapePoints) {
      return shapePoints.get(id);
    }
    return getEntityForId(ShapePoint.class, id);
  }

  public Stop getStopForId(AgencyAndId id) {
    return getEntityForId(Stop.class, id);
  }

  public StopTime getStopTimeForId(int id) {
    if (packStopTimes) {
      return stopTimes.get(id);
    }
    return getEntityForId(StopTime.class, id);
  }

  public Transfer getTransferForId(int id) {
    return getEntityForId(Transfer.class, id);
  }

  public Trip getTripForId(AgencyAndId id) {
    return getEntityForId(Trip.class, id);
  }

  /****
   * {@link GenericMutableDao} Interface
   ****/

  @Override
  public <K, V> Map<K, V> getEntitiesByIdForEntityType(Class<K> keyType,
      Class<V> entityType) {
    noKeyCheck(keyType);
    return super.getEntitiesByIdForEntityType(keyType, entityType);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Collection<T> getAllEntitiesForType(Class<T> type) {
    if (packStopTimes && type.equals(StopTime.class)) {
      return (Collection<T>) stopTimes;
    } else if (packShapePoints && type.equals(ShapePoint.class)) {
      return (Collection<T>) shapePoints;
    }
    return super.getAllEntitiesForType(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getEntityForId(Class<T> type, Serializable id) {
    if (packStopTimes && type.equals(StopTime.class)) {
      return (T) stopTimes.get((Integer) id);
    } else if (packShapePoints && type.equals(ShapePoint.class)) {
      return (T) shapePoints.get((Integer) id);
    }
    return super.getEntityForId(type, id);
  }

  @Override
  public void saveEntity(Object entity) {
    if (packStopTimes && entity.getClass().equals(StopTime.class)) {
      stopTimes.add((StopTime) entity);
      return;
    } else if (packShapePoints && entity.getClass().equals(ShapePoint.class)) {
      shapePoints.add((ShapePoint) entity);
      return;
    }
    super.saveEntity(entity);
  }

  @Override
  public <T> void clearAllEntitiesForType(Class<T> type) {
    if (packStopTimes && type.equals(StopTime.class)) {
      stopTimes.clear();
      return;
    } else if (packShapePoints && type.equals(ShapePoint.class)) {
      shapePoints.clear();
      return;
    }
    super.clearAllEntitiesForType(type);
  }

  @Override
  public <K extends Serializable, T extends IdentityBean<K>> void removeEntity(
      T entity) {
    if (packStopTimes && entity.getClass().equals(StopTime.class)) {
      throw new UnsupportedOperationException();
    } else if (packShapePoints && entity.getClass().equals(ShapePoint.class)) {
      throw new UnsupportedOperationException();
    }
    super.removeEntity(entity);
  }

  @Override
  public void close() {
    if (packStopTimes) {
      stopTimes.trimToSize();
    }
    if (packShapePoints) {
      shapePoints.trimToSize();
    }
    super.close();
  }

  /****
   * Private Methods
   ****/

  private <K> void noKeyCheck(Class<K> keyType) {
    if (packStopTimes && keyType.equals(StopTime.class)) {
      throw new UnsupportedOperationException();
    }
    if (packShapePoints && keyType.equals(ShapePoint.class)) {
      throw new UnsupportedOperationException();
    }
  }

}
