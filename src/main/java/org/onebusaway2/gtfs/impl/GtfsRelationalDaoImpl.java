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
package org.onebusaway.gtfs.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.csv_entities.exceptions.EntityInstantiationException;
import org.onebusaway.csv_entities.schema.BeanWrapper;
import org.onebusaway.csv_entities.schema.BeanWrapperFactory;
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
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;

/**
 * A in-memory implementation of GtfsRelationalDaoImpl. It's super fast for most
 * methods, but only if you have enough memory to load your entire GTFS into
 * memory.
 * 
 * @author bdferris
 * 
 */
public class GtfsRelationalDaoImpl extends GtfsDaoImpl implements
    GtfsMutableRelationalDao {

  private Map<AgencyAndId, List<String>> _tripAgencyIdsByServiceId = null;

  private Map<Agency, List<Route>> _routesByAgency = null;

  private Map<Stop, List<Stop>> _stopsByStation = null;

  private Map<Trip, List<StopTime>> _stopTimesByTrip = null;

  private Map<Stop, List<StopTime>> _stopTimesByStop = null;

  private Map<Route, List<Trip>> _tripsByRoute = null;

  private Map<AgencyAndId, List<Trip>> _tripsByShapeId = null;

  private Map<AgencyAndId, List<Trip>> _tripsByServiceId = null;

  private Map<AgencyAndId, List<Trip>> _tripsByBlockId = null;

  private Map<AgencyAndId, List<ShapePoint>> _shapePointsByShapeId = null;

  private Map<Trip, List<Frequency>> _frequenciesByTrip = null;

  private Map<AgencyAndId, List<ServiceCalendarDate>> _calendarDatesByServiceId = null;

  private Map<AgencyAndId, List<ServiceCalendar>> _calendarsByServiceId = null;

  private Map<FareAttribute, List<FareRule>> _fareRulesByFareAttribute = null;

  public void clearAllCaches() {
    _tripAgencyIdsByServiceId = clearMap(_tripAgencyIdsByServiceId);
    _routesByAgency = clearMap(_routesByAgency);
    _stopsByStation = clearMap(_stopsByStation);
    _stopTimesByTrip = clearMap(_stopTimesByTrip);
    _stopTimesByStop = clearMap(_stopTimesByStop);
    _tripsByRoute = clearMap(_tripsByRoute);
    _tripsByShapeId = clearMap(_tripsByShapeId);
    _tripsByServiceId = clearMap(_tripsByServiceId);
    _tripsByBlockId = clearMap(_tripsByBlockId);
    _shapePointsByShapeId = clearMap(_shapePointsByShapeId);
    _frequenciesByTrip = clearMap(_frequenciesByTrip);
    _calendarDatesByServiceId = clearMap(_calendarDatesByServiceId);
    _calendarsByServiceId = clearMap(_calendarsByServiceId);
    _fareRulesByFareAttribute = clearMap(_fareRulesByFareAttribute);
  }

  @Override
  public List<String> getTripAgencyIdsReferencingServiceId(AgencyAndId serviceId) {

    if (_tripAgencyIdsByServiceId == null) {

      Map<AgencyAndId, Set<String>> agencyIdsByServiceIds = new HashMap<AgencyAndId, Set<String>>();

      for (Trip trip : getAllTrips()) {
        AgencyAndId tripId = trip.getId();
        String tripAgencyId = tripId.getAgencyId();
        AgencyAndId tripServiceId = trip.getServiceId();
        Set<String> agencyIds = agencyIdsByServiceIds.get(tripServiceId);
        if (agencyIds == null) {
          agencyIds = new HashSet<String>();
          agencyIdsByServiceIds.put(tripServiceId, agencyIds);
        }
        agencyIds.add(tripAgencyId);
      }

      _tripAgencyIdsByServiceId = new HashMap<AgencyAndId, List<String>>();

      for (Map.Entry<AgencyAndId, Set<String>> entry : agencyIdsByServiceIds.entrySet()) {
        AgencyAndId tripServiceId = entry.getKey();
        List<String> agencyIds = new ArrayList<String>(entry.getValue());
        Collections.sort(agencyIds);
        _tripAgencyIdsByServiceId.put(tripServiceId, agencyIds);
      }
    }

    List<String> agencyIds = _tripAgencyIdsByServiceId.get(serviceId);
    if (agencyIds == null)
      agencyIds = new ArrayList<String>();
    return agencyIds;
  }

  @Override
  public List<Route> getRoutesForAgency(Agency agency) {
    if (_routesByAgency == null)
      _routesByAgency = mapToValueList(getAllRoutes(), "agency", Agency.class);
    return list(_routesByAgency.get(agency));
  }

  @Override
  public List<Stop> getStopsForStation(Stop station) {
    if (_stopsByStation == null) {
      _stopsByStation = new HashMap<Stop, List<Stop>>();
      for (Stop stop : getAllStops()) {
        if (stop.getLocationType() == 0 && stop.getParentStation() != null) {
          Stop parentStation = getStopForId(new AgencyAndId(
              stop.getId().getAgencyId(), stop.getParentStation()));
          List<Stop> subStops = _stopsByStation.get(parentStation);
          if (subStops == null) {
            subStops = new ArrayList<Stop>(2);
            _stopsByStation.put(parentStation, subStops);
          }
          subStops.add(stop);
        }
      }
    }
    return list(_stopsByStation.get(station));
  }

  @Override
  public List<AgencyAndId> getAllShapeIds() {
    ensureShapePointRelation();
    return new ArrayList<AgencyAndId>(_shapePointsByShapeId.keySet());
  }

  @Override
  public List<ShapePoint> getShapePointsForShapeId(AgencyAndId shapeId) {
    ensureShapePointRelation();
    return list(_shapePointsByShapeId.get(shapeId));
  }

  @Override
  public List<StopTime> getStopTimesForTrip(Trip trip) {

    if (_stopTimesByTrip == null) {
      _stopTimesByTrip = mapToValueList(getAllStopTimes(), "trip", Trip.class);
      for (List<StopTime> stopTimes : _stopTimesByTrip.values())
        Collections.sort(stopTimes);
    }

    return list(_stopTimesByTrip.get(trip));
  }

  @Override
  public List<StopTime> getStopTimesForStop(Stop stop) {
    if (_stopTimesByStop == null)
      _stopTimesByStop = mapToValueList(getAllStopTimes(), "stop", Stop.class);
    return list(_stopTimesByStop.get(stop));
  }

  @Override
  public List<Trip> getTripsForRoute(Route route) {
    if (_tripsByRoute == null)
      _tripsByRoute = mapToValueList(getAllTrips(), "route", Route.class);
    return list(_tripsByRoute.get(route));
  }

  @Override
  public List<Trip> getTripsForShapeId(AgencyAndId shapeId) {
    if (_tripsByShapeId == null) {
      _tripsByShapeId = mapToValueList(getAllTrips(), "shapeId",
          AgencyAndId.class);
    }
    return list(_tripsByShapeId.get(shapeId));
  }

  @Override
  public List<Trip> getTripsForServiceId(AgencyAndId serviceId) {
    if (_tripsByServiceId == null) {
      _tripsByServiceId = mapToValueList(getAllTrips(), "serviceId",
          AgencyAndId.class);
    }
    return list(_tripsByServiceId.get(serviceId));
  }

  @Override
  public List<Trip> getTripsForBlockId(AgencyAndId blockId) {

    if (_tripsByBlockId == null) {
      _tripsByBlockId = new HashMap<AgencyAndId, List<Trip>>();
      for (Trip trip : getAllTrips()) {
        if (trip.getBlockId() != null) {
          AgencyAndId bid = new AgencyAndId(trip.getId().getAgencyId(),
              trip.getBlockId());
          List<Trip> trips = _tripsByBlockId.get(bid);
          if (trips == null) {
            trips = new ArrayList<Trip>();
            _tripsByBlockId.put(bid, trips);
          }
          trips.add(trip);
        }
      }
    }

    return list(_tripsByBlockId.get(blockId));
  }

  @Override
  public List<Frequency> getFrequenciesForTrip(Trip trip) {
    if (_frequenciesByTrip == null)
      _frequenciesByTrip = mapToValueList(getAllFrequencies(), "trip",
          Trip.class);
    return list(_frequenciesByTrip.get(trip));
  }

  @Override
  public List<AgencyAndId> getAllServiceIds() {
    ensureCalendarDatesByServiceIdRelation();
    ensureCalendarsByServiceIdRelation();
    Set<AgencyAndId> serviceIds = new HashSet<AgencyAndId>();
    serviceIds.addAll(_calendarDatesByServiceId.keySet());
    serviceIds.addAll(_calendarsByServiceId.keySet());
    return new ArrayList<AgencyAndId>(serviceIds);
  }

  @Override
  public List<ServiceCalendarDate> getCalendarDatesForServiceId(
      AgencyAndId serviceId) {
    ensureCalendarDatesByServiceIdRelation();
    return list(_calendarDatesByServiceId.get(serviceId));
  }

  @Override
  public ServiceCalendar getCalendarForServiceId(AgencyAndId serviceId) {
    ensureCalendarsByServiceIdRelation();
    List<ServiceCalendar> calendars = list(_calendarsByServiceId.get(serviceId));
    switch (calendars.size()) {
      case 0:
        return null;
      case 1:
        return calendars.get(0);
    }
    throw new MultipleCalendarsForServiceIdException(serviceId);
  }

  @Override
  public List<FareRule> getFareRulesForFareAttribute(FareAttribute fareAttribute) {
    if (_fareRulesByFareAttribute == null) {
      _fareRulesByFareAttribute = mapToValueList(getAllFareRules(), "fare",
          FareAttribute.class);
    }
    return list(_fareRulesByFareAttribute.get(fareAttribute));
  }

  /****
   * Private Methods
   ****/

  private void ensureCalendarDatesByServiceIdRelation() {
    if (_calendarDatesByServiceId == null) {
      _calendarDatesByServiceId = mapToValueList(getAllCalendarDates(),
          "serviceId", AgencyAndId.class);
    }
  }

  private void ensureCalendarsByServiceIdRelation() {
    if (_calendarsByServiceId == null) {
      _calendarsByServiceId = mapToValueList(getAllCalendars(), "serviceId",
          AgencyAndId.class);
    }
  }

  private void ensureShapePointRelation() {
    if (_shapePointsByShapeId == null) {
      _shapePointsByShapeId = mapToValueList(getAllShapePoints(), "shapeId",
          AgencyAndId.class);
      for (List<ShapePoint> shapePoints : _shapePointsByShapeId.values())
        Collections.sort(shapePoints);
    }
  }

  private static <T> List<T> list(List<T> list) {
    if (list == null)
      list = new ArrayList<T>();
    return Collections.unmodifiableList(list);
  }

  @SuppressWarnings("unchecked")
  private static <K, V> Map<K, List<V>> mapToValueList(Iterable<V> values,
      String property, Class<K> keyType) {
    return mapToValueCollection(values, property, keyType,
        new ArrayList<V>().getClass());
  }

  @SuppressWarnings("unchecked")
  private static <K, V, C extends Collection<V>, CIMPL extends C> Map<K, C> mapToValueCollection(
      Iterable<V> values, String property, Class<K> keyType,
      Class<CIMPL> collectionType) {

    Map<K, C> byKey = new HashMap<K, C>();
    SimplePropertyQuery query = new SimplePropertyQuery(property);

    for (V value : values) {

      K key = (K) query.invoke(value);
      C valuesForKey = byKey.get(key);
      if (valuesForKey == null) {

        try {
          valuesForKey = collectionType.newInstance();
        } catch (Exception ex) {
          throw new EntityInstantiationException(collectionType, ex);
        }

        byKey.put(key, valuesForKey);
      }
      valuesForKey.add(value);
    }

    return byKey;
  }

  private <K, V> Map<K, V> clearMap(Map<K, V> map) {
    if (map != null)
      map.clear();
    return null;
  }

  private static final class SimplePropertyQuery {

    private String[] _properties;

    public SimplePropertyQuery(String query) {
      _properties = query.split("\\.");
    }

    public Object invoke(Object value) {
      for (String property : _properties) {
        BeanWrapper wrapper = BeanWrapperFactory.wrap(value);
        value = wrapper.getPropertyValue(property);
      }
      return value;
    }
  }

}
