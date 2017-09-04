/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2011 Google, Inc.
 * Copyright (C) 2011 Laurent Gregoire <laurent.gregoire@gmail.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway2.gtfs.impl;

import org.onebusaway2.gtfs.model.*;
import org.onebusaway2.gtfs.services.GtfsDao;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A in-memory implementation of GtfsDao. It's super fast for most
 * methods, but only if you have enough memory to load your entire GTFS into
 * memory.
 *
 * @author bdferris
 */
public class GtfsDaoImpl extends GenericDaoImpl implements GtfsDao {

    private Map<AgencyAndId, List<String>> tripAgencyIdsByServiceId = null;
    private Map<Stop, List<Stop>> stopsByStation = null;
    private Map<Trip, List<StopTime>> stopTimesByTrip = null;
    private Map<AgencyAndId, List<ShapePoint>> shapePointsByShapeId = null;
    private Map<AgencyAndId, List<ServiceCalendarDate>> calendarDatesByServiceId = null;
    private Map<AgencyAndId, List<ServiceCalendar>> calendarsByServiceId = null;



    public GtfsDaoImpl(
            Collection<Agency> agencies,
            Collection<ServiceCalendarDate> calendarDates,
            Collection<ServiceCalendar> calendars,
            Collection<FareAttribute> fareAttributes,
            Collection<FareRule> fareRules,
            Collection<FeedInfo> feedInfos,
            Collection<Frequency> frequencies,
            Collection<Pathway> pathways,
            Collection<Route> routes,
            Collection<ShapePoint> shapePoints,
            Collection<Stop> stops,
            Collection<StopTime> stopTimes,
            Collection<Transfer> transfers,
            Collection<Trip> trips
    ) {
        saveAll(agencies);
        saveAll(calendarDates);
        saveAll(calendars);
        saveAll(fareAttributes);
        saveAll(fareRules);
        saveAll(feedInfos);
        saveAll(frequencies);
        saveAll(pathways);
        saveAll(routes);
        saveAll(shapePoints);
        saveAll(stops);
        saveAll(stopTimes);
        saveAll(transfers);
        saveAll(trips);
    }

    @Override public Collection<Agency> getAllAgencies() {
        return getAllEntitiesForType(Agency.class);
    }

    @Override public Collection<ServiceCalendarDate> getAllCalendarDates() {
        return getAllEntitiesForType(ServiceCalendarDate.class);
    }

    @Override public Collection<ServiceCalendar> getAllCalendars() {
        return getAllEntitiesForType(ServiceCalendar.class);
    }

    @Override public Collection<FareAttribute> getAllFareAttributes() {
        return getAllEntitiesForType(FareAttribute.class);
    }

    @Override public Collection<FareRule> getAllFareRules() {
        return getAllEntitiesForType(FareRule.class);
    }

    @Override public Collection<FeedInfo> getAllFeedInfos() {
        return getAllEntitiesForType(FeedInfo.class);
    }

    @Override public Collection<Frequency> getAllFrequencies() {
        return getAllEntitiesForType(Frequency.class);
    }

    @Override public Collection<Route> getAllRoutes() {
        return getAllEntitiesForType(Route.class);
    }

    @Override public Collection<ShapePoint> getAllShapePoints() {
        return getAllEntitiesForType(ShapePoint.class);
    }

    @Override public Collection<StopTime> getAllStopTimes() {
        return super.getAllEntitiesForType(StopTime.class);
    }

    @Override public Collection<Stop> getAllStops() {
        return getAllEntitiesForType(Stop.class);
    }

    @Override public Collection<Transfer> getAllTransfers() {
        return getAllEntitiesForType(Transfer.class);
    }

    @Override public Collection<Trip> getAllTrips() {
        return getAllEntitiesForType(Trip.class);
    }

    @Override public Collection<Pathway> getAllPathways() {
        return getAllEntitiesForType(Pathway.class);
    }

    @Override public Stop getStopForId(AgencyAndId id) {
        return getEntityForId(Stop.class, id);
    }

    @Override public List<String> getTripAgencyIdsReferencingServiceId(AgencyAndId serviceId) {

        if (tripAgencyIdsByServiceId == null) {

            Map<AgencyAndId, Set<String>> agencyIdsByServiceIds = new HashMap<>();

            for (Trip trip : getAllTrips()) {
                AgencyAndId tripId = trip.getId();
                String tripAgencyId = tripId.getAgencyId();
                AgencyAndId tripServiceId = trip.getServiceId();
                Set<String> agencyIds = agencyIdsByServiceIds.computeIfAbsent(
                        tripServiceId,
                        k -> new HashSet<>()
                );
                agencyIds.add(tripAgencyId);
            }

            tripAgencyIdsByServiceId = new HashMap<>();

            for (Map.Entry<AgencyAndId, Set<String>> entry : agencyIdsByServiceIds.entrySet()) {
                AgencyAndId tripServiceId = entry.getKey();
                List<String> agencyIds = new ArrayList<>(entry.getValue());
                Collections.sort(agencyIds);
                tripAgencyIdsByServiceId.put(tripServiceId, agencyIds);
            }
        }

        return list(tripAgencyIdsByServiceId.get(serviceId));
    }

    @Override public List<Stop> getStopsForStation(Stop station) {
        if (stopsByStation == null) {
            stopsByStation = new HashMap<>();
            for (Stop stop : getAllStops()) {
                if (stop.getLocationType() == 0 && stop.getParentStation() != null) {
                    Stop parentStation = getStopForId(
                            new AgencyAndId(stop.getId().getAgencyId(), stop.getParentStation()));
                    List<Stop> subStops = stopsByStation.computeIfAbsent(
                            parentStation,
                            k -> new ArrayList<>(2)
                    );
                    subStops.add(stop);
                }
            }
        }
        return list(stopsByStation.get(station));
    }

    @Override public List<ShapePoint> getShapePointsForShapeId(AgencyAndId shapeId) {
        ensureShapePointRelation();
        return list(shapePointsByShapeId.get(shapeId));
    }

    @Override public List<StopTime> getStopTimesForTrip(Trip trip) {

        if (stopTimesByTrip == null) {
            stopTimesByTrip = getAllStopTimes().stream().collect(Collectors.groupingBy(StopTime::getTrip));

            for (List<StopTime> stopTimes : stopTimesByTrip.values()) {
                Collections.sort(stopTimes);
            }
        }

        return list(stopTimesByTrip.get(trip));
    }

    @Override public List<AgencyAndId> getAllServiceIds() {
        ensureCalendarDatesByServiceIdRelation();
        ensureCalendarsByServiceIdRelation();
        Set<AgencyAndId> serviceIds = new HashSet<>();
        serviceIds.addAll(calendarDatesByServiceId.keySet());
        serviceIds.addAll(calendarsByServiceId.keySet());
        return new ArrayList<>(serviceIds);
    }

    @Override public List<ServiceCalendarDate> getCalendarDatesForServiceId(AgencyAndId serviceId) {
        ensureCalendarDatesByServiceIdRelation();
        return list(calendarDatesByServiceId.get(serviceId));
    }

    @Override public ServiceCalendar getCalendarForServiceId(AgencyAndId serviceId) {
        ensureCalendarsByServiceIdRelation();
        List<ServiceCalendar> calendars = list(calendarsByServiceId.get(serviceId));
        switch (calendars.size()) {
        case 0:
            return null;
        case 1:
            return calendars.get(0);
        }
        throw new MultipleCalendarsForServiceIdException(serviceId);
    }


    /*  Private Methods */

    private void saveAll(Collection<?> entities) {
        for (Object it : entities) {
            saveEntity(it);
        }
    }

    private void ensureCalendarDatesByServiceIdRelation() {
        if (calendarDatesByServiceId == null) {
            calendarDatesByServiceId = getAllCalendarDates().stream().collect(
                    Collectors.groupingBy(ServiceCalendarDate::getServiceId)
            );
        }
    }

    private void ensureCalendarsByServiceIdRelation() {
        if (calendarsByServiceId == null) {
            calendarsByServiceId =  getAllCalendars().stream().collect(
                    Collectors.groupingBy(ServiceCalendar::getServiceId)
            );
        }
    }

    private void ensureShapePointRelation() {
        if (shapePointsByShapeId == null) {
            shapePointsByShapeId = getAllShapePoints().stream().collect(
                    Collectors.groupingBy(ShapePoint::getShapeId)
            );
            for(List<ShapePoint> list : shapePointsByShapeId.values()) {
                Collections.sort(list);
            }
        }
    }

    private static <T> List<T> list(List<T> list) {
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }
}
