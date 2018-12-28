/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.impl;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FlexArea;
import org.opentripplanner.model.FeedScopedId;
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
 * A in-memory implementation of {@link OtpTransitService}. It's super fast for most
 * methods, but only if you have enough memory to load your entire {@link OtpTransitService}
 * into memory.
 * <p>
 * This class is read only, to enforce consistency after generating indexes and ids.
 * You will get an exception if you try to add entities to one of the collections.
 * If you need to modify a {@link OtpTransitService}, you can create a new
 * {@link OtpTransitServiceBuilder} based on your old data, do your modification and
 * create a new unmodifiable instance.
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

    private Map<FeedScopedId, Stop> stops;

    private Collection<StopTime> stopTimes;

    private Collection<Transfer> transfers;

    private Collection<Trip> trips;

    private Collection<FlexArea> flexAreas;

    // Indexes
    private Map<FeedScopedId, List<String>> tripAgencyIdsByServiceId = null;

    private Map<Stop, List<Stop>> stopsByStation = null;

    private Map<Trip, List<StopTime>> stopTimesByTrip = null;

    private Map<FeedScopedId, List<ShapePoint>> shapePointsByShapeId = null;

    private Map<FeedScopedId, List<ServiceCalendarDate>> calendarDatesByServiceId = null;

    private Map<FeedScopedId, List<ServiceCalendar>> calendarsByServiceId = null;

    /**
     * Create a read only version of the {@link OtpTransitService}.
     * @see OtpTransitServiceBuilder Use builder to mutate the instance.
     */
    OtpTransitServiceImpl(List<Agency> agencies, List<ServiceCalendarDate> calendarDates,
            List<ServiceCalendar> calendars, List<FareAttribute> fareAttributes,
            List<FareRule> fareRules, List<FeedInfo> feedInfos, List<Frequency> frequencies,
            List<Pathway> pathways, List<Route> routes, List<ShapePoint> shapePoints,
            List<Stop> stops, List<StopTime> stopTimes, List<Transfer> transfers,
            List<Trip> trips, List<FlexArea> flexAreas) {
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
        this.flexAreas = nullSafeUnmodifiableList(flexAreas);
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
    public Stop getStopForId(FeedScopedId id) {
        return stops.get(id);
    }

    @Override
    public List<String> getTripAgencyIdsReferencingServiceId(FeedScopedId serviceId) {

        if (tripAgencyIdsByServiceId == null) {

            Map<FeedScopedId, Set<String>> agencyIdsByServiceIds = new HashMap<>();

            for (Trip trip : getAllTrips()) {
                FeedScopedId tripId = trip.getId();
                String tripAgencyId = tripId.getAgencyId();
                FeedScopedId tripServiceId = trip.getServiceId();
                Set<String> agencyIds = agencyIdsByServiceIds
                        .computeIfAbsent(tripServiceId, k -> new HashSet<>());
                agencyIds.add(tripAgencyId);
            }

            tripAgencyIdsByServiceId = new HashMap<>();

            for (Map.Entry<FeedScopedId, Set<String>> entry : agencyIdsByServiceIds.entrySet()) {
                FeedScopedId tripServiceId = entry.getKey();
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
                            new FeedScopedId(stop.getId().getAgencyId(), stop.getParentStation()));
                    List<Stop> subStops = stopsByStation
                            .computeIfAbsent(parentStation, k -> new ArrayList<>(2));
                    subStops.add(stop);
                }
            }
        }
        return nullSafeUnmodifiableList(stopsByStation.get(station));
    }

    @Override
    public List<ShapePoint> getShapePointsForShapeId(FeedScopedId shapeId) {
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
    public List<FeedScopedId> getAllServiceIds() {
        ensureCalendarDatesByServiceIdRelation();
        ensureCalendarsByServiceIdRelation();
        Set<FeedScopedId> serviceIds = new HashSet<>();
        serviceIds.addAll(calendarDatesByServiceId.keySet());
        serviceIds.addAll(calendarsByServiceId.keySet());
        return new ArrayList<>(serviceIds);
    }

    @Override
    public List<ServiceCalendarDate> getCalendarDatesForServiceId(FeedScopedId serviceId) {
        ensureCalendarDatesByServiceIdRelation();
        return nullSafeUnmodifiableList(calendarDatesByServiceId.get(serviceId));
    }

    @Override
    public ServiceCalendar getCalendarForServiceId(FeedScopedId serviceId) {
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

    @Override
    public Collection<FlexArea> getAllAreas() {
        return flexAreas;
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
