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
package org.opentripplanner.model.impl;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.OtpTransitService;

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
 * A in-memory implementation of OtpTransitDao. It's super fast for most
 * methods, but only if you have enough memory to load your entire OtpTransitDao
 * into memory.
 * <p>
 * The Dao is read only, to enforece consistency after generating indexes and ids.
 * You will get an exception if you try to add entities to one of the collections.
 * If you need to modify a {@link OtpTransitService}, you can create a new
 * {@link OtpTransitBuilder} based on your old data, do your modification and
 * create a new unmodifiable dao.
 *
 * @author bdferris
 */
class OtpTransitServiceImpl implements OtpTransitService {

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

    /**
     * Create a read only version of the OtpTransitDao.
     * @see OtpTransitBuilder Use builder to create an new OtpTransitDao.
     */
    OtpTransitServiceImpl(List<Agency> agencies, List<ServiceCalendarDate> calendarDates,
            List<ServiceCalendar> calendars, List<FareAttribute> fareAttributes,
            List<FareRule> fareRules, List<FeedInfo> feedInfos, List<Frequency> frequencies,
            List<Pathway> pathways, List<Route> routes, List<ShapePoint> shapePoints,
            List<Stop> stops, List<StopTime> stopTimes, List<Transfer> transfers,
            List<Trip> trips) {
        this.agencies = nullSafeUnmodifiableList(agencies);
        this.calendarDates = nullSafeUnmodifiableList(calendarDates);
        this.calendars = nullSafeUnmodifiableList(calendars);
        this.fareAttributes = nullSafeUnmodifiableList(fareAttributes);
        this.fareRules = nullSafeUnmodifiableList(fareRules);
        this.feedInfos = nullSafeUnmodifiableList(feedInfos);
        this.frequencies = nullSafeUnmodifiableList(frequencies);
        this.pathways = nullSafeUnmodifiableList(pathways);
        this.routes = nullSafeUnmodifiableList(routes);
        this.shapePoints = nullSafeUnmodifiableList(shapePoints);
        this.stops = stops.stream().collect(toMap(Stop::getId, identity()));
        this.stopTimes = nullSafeUnmodifiableList(stopTimes);
        this.transfers = nullSafeUnmodifiableList(transfers);
        this.trips = nullSafeUnmodifiableList(trips);
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

        return nullSafeUnmodifiableList(tripAgencyIdsByServiceId.get(serviceId));
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
        return nullSafeUnmodifiableList(stopsByStation.get(station));
    }

    @Override
    public List<ShapePoint> getShapePointsForShapeId(AgencyAndId shapeId) {
        ensureShapePointRelation();
        return nullSafeUnmodifiableList(shapePointsByShapeId.get(shapeId));
    }

    @Override
    public List<StopTime> getStopTimesForTrip(Trip trip) {

        if (stopTimesByTrip == null) {
            stopTimesByTrip = getAllStopTimes().stream().collect(groupingBy(StopTime::getTrip));

            for (List<StopTime> stopTimes : stopTimesByTrip.values()) {
                Collections.sort(stopTimes);
            }
        }

        return nullSafeUnmodifiableList(stopTimesByTrip.get(trip));
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
        return nullSafeUnmodifiableList(calendarDatesByServiceId.get(serviceId));
    }

    @Override
    public ServiceCalendar getCalendarForServiceId(AgencyAndId serviceId) {
        ensureCalendarsByServiceIdRelation();
        List<ServiceCalendar> calendars = calendarsByServiceId.get(serviceId);

        if(calendars == null || calendars.isEmpty()) {
            return null;
        }
        if(calendars.size() == 1) {
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

    private static <T> List<T> nullSafeUnmodifiableList(List<T> list) {
        return Collections.unmodifiableList(list == null ? Collections.emptyList() : list);
    }
}
