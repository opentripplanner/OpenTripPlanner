/*
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

import org.onebusaway2.gtfs.model.Agency;
import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.FareAttribute;
import org.onebusaway2.gtfs.model.FareRule;
import org.onebusaway2.gtfs.model.FeedInfo;
import org.onebusaway2.gtfs.model.Frequency;
import org.onebusaway2.gtfs.model.IdentityBean;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * A in-memory implementation of GtfsDao. It's super fast for most
 * methods, but only if you have enough memory to load your entire GTFS into
 * memory.
 *
 * @author bdferris
 */
public class GtfsDaoImpl implements GtfsDao {

    private Collection<Agency> agencies;

    private Collection<ServiceCalendarDate> calendarDates;

    private Collection<ServiceCalendar> calendars;

    private Collection<FareAttribute> fareAttributes;

    private Collection<FareRule> fareRules;

    private Collection<FeedInfo> feedInfos;

    private Collection<Frequency> frequencies;

    private Collection<Pathway> pathways;

    private Collection<Route> routes;

    private Collection<ShapePoint> shapePoints;

    private Map<AgencyAndId, Stop> stops;

    private Collection<StopTime> stopTimes;

    private Collection<Transfer> transfers;

    private Collection<Trip> trips;

    // Indexes
    private Map<AgencyAndId, List<String>> tripAgencyIdsByServiceId = null;

    private Map<Stop, List<Stop>> stopsByStation = null;

    private Map<Trip, List<StopTime>> stopTimesByTrip = null;

    private Map<AgencyAndId, List<ShapePoint>> shapePointsByShapeId = null;

    private Map<AgencyAndId, List<ServiceCalendarDate>> calendarDatesByServiceId = null;

    private Map<AgencyAndId, List<ServiceCalendar>> calendarsByServiceId = null;

    public GtfsDaoImpl(Collection<Agency> agencies, Collection<ServiceCalendarDate> calendarDates,
            Collection<ServiceCalendar> calendars, Collection<FareAttribute> fareAttributes,
            Collection<FareRule> fareRules, Collection<FeedInfo> feedInfos,
            Collection<Frequency> frequencies, Collection<Pathway> pathways,
            Collection<Route> routes, Collection<ShapePoint> shapePoints, Collection<Stop> stops,
            Collection<StopTime> stopTimes, Collection<Transfer> transfers,
            Collection<Trip> trips) {
        this.agencies = agencies;
        this.calendarDates = insertIds(calendarDates);
        this.calendars = insertIds(calendars);
        this.fareAttributes = fareAttributes;
        this.fareRules = insertIds(fareRules);
        this.feedInfos = insertIds(feedInfos);
        this.frequencies = insertIds(frequencies);
        this.pathways = pathways;
        this.routes = routes;
        this.shapePoints = shapePoints;
        this.stops = stops.stream().collect(toMap(Stop::getId, identity()));
        this.stopTimes = insertIds(stopTimes);
        this.transfers = insertIds(transfers);
        this.trips = trips;
    }

    @Override
    public Collection<Agency> getAllAgencies() {
        return agencies;
    }

    @Override
    public Collection<ServiceCalendarDate> getAllCalendarDates() {
        return calendarDates;
    }

    @Override
    public Collection<ServiceCalendar> getAllCalendars() {
        return calendars;
    }

    @Override
    public Collection<FareAttribute> getAllFareAttributes() {
        return fareAttributes;
    }

    @Override
    public Collection<FareRule> getAllFareRules() {
        return fareRules;
    }

    @Override
    public Collection<FeedInfo> getAllFeedInfos() {
        return feedInfos;
    }

    @Override
    public Collection<Frequency> getAllFrequencies() {
        return frequencies;
    }

    @Override
    public Collection<Route> getAllRoutes() {
        return routes;
    }

    @Override
    public Collection<ShapePoint> getAllShapePoints() {
        return shapePoints;
    }

    @Override
    public Collection<StopTime> getAllStopTimes() {
        return stopTimes;
    }

    @Override
    public Collection<Stop> getAllStops() {
        return stops.values();
    }

    @Override
    public Collection<Transfer> getAllTransfers() {
        return transfers;
    }

    @Override
    public Collection<Trip> getAllTrips() {
        return trips;
    }

    @Override
    public Collection<Pathway> getAllPathways() {
        return pathways;
    }

    @Override
    public Stop getStopForId(AgencyAndId id) {
        return stops.get(id);
    }

    @Override
    public List<String> getTripAgencyIdsReferencingServiceId(AgencyAndId serviceId) {

        if (tripAgencyIdsByServiceId == null) {

            Map<AgencyAndId, Set<String>> agencyIdsByServiceIds = new HashMap<>();

            for (Trip trip : getAllTrips()) {
                AgencyAndId tripId = trip.getId();
                String tripAgencyId = tripId.getAgencyId();
                AgencyAndId tripServiceId = trip.getServiceId();
                Set<String> agencyIds = agencyIdsByServiceIds
                        .computeIfAbsent(tripServiceId, k -> new HashSet<>());
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

    @Override
    public List<Stop> getStopsForStation(Stop station) {
        if (stopsByStation == null) {
            stopsByStation = new HashMap<>();
            for (Stop stop : getAllStops()) {
                if (stop.getLocationType() == 0 && stop.getParentStation() != null) {
                    Stop parentStation = getStopForId(
                            new AgencyAndId(stop.getId().getAgencyId(), stop.getParentStation()));
                    List<Stop> subStops = stopsByStation
                            .computeIfAbsent(parentStation, k -> new ArrayList<>(2));
                    subStops.add(stop);
                }
            }
        }
        return list(stopsByStation.get(station));
    }

    @Override
    public List<ShapePoint> getShapePointsForShapeId(AgencyAndId shapeId) {
        ensureShapePointRelation();
        return list(shapePointsByShapeId.get(shapeId));
    }

    @Override
    public List<StopTime> getStopTimesForTrip(Trip trip) {

        if (stopTimesByTrip == null) {
            stopTimesByTrip = getAllStopTimes().stream().collect(groupingBy(StopTime::getTrip));

            for (List<StopTime> stopTimes : stopTimesByTrip.values()) {
                Collections.sort(stopTimes);
            }
        }

        return list(stopTimesByTrip.get(trip));
    }

    @Override
    public List<AgencyAndId> getAllServiceIds() {
        ensureCalendarDatesByServiceIdRelation();
        ensureCalendarsByServiceIdRelation();
        Set<AgencyAndId> serviceIds = new HashSet<>();
        serviceIds.addAll(calendarDatesByServiceId.keySet());
        serviceIds.addAll(calendarsByServiceId.keySet());
        return new ArrayList<>(serviceIds);
    }

    @Override
    public List<ServiceCalendarDate> getCalendarDatesForServiceId(AgencyAndId serviceId) {
        ensureCalendarDatesByServiceIdRelation();
        return list(calendarDatesByServiceId.get(serviceId));
    }

    @Override
    public ServiceCalendar getCalendarForServiceId(AgencyAndId serviceId) {
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

    private void ensureCalendarDatesByServiceIdRelation() {
        if (calendarDatesByServiceId == null) {
            calendarDatesByServiceId = getAllCalendarDates().stream()
                    .collect(groupingBy(ServiceCalendarDate::getServiceId));
        }
    }

    private void ensureCalendarsByServiceIdRelation() {
        if (calendarsByServiceId == null) {
            calendarsByServiceId = getAllCalendars().stream()
                    .collect(groupingBy(ServiceCalendar::getServiceId));
        }
    }

    private void ensureShapePointRelation() {
        if (shapePointsByShapeId == null) {
            shapePointsByShapeId = getAllShapePoints().stream()
                    .collect(groupingBy(ShapePoint::getShapeId));
            for (List<ShapePoint> list : shapePointsByShapeId.values()) {
                Collections.sort(list);
            }
        }
    }

    private static <T> List<T> list(List<T> list) {
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    private static <T extends IdentityBean<Integer>> Collection<T> insertIds(Collection<T> dates) {
        return new ArrayListWithIdGeneration<>(dates);
    }
}
