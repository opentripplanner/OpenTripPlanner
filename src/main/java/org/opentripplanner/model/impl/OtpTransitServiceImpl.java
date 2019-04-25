/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.impl;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

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

    private final Collection<Agency> agencies;

    private final Collection<FareAttribute> fareAttributes;

    private final Collection<FareRule> fareRules;

    private final Collection<FeedInfo> feedInfos;

    private final Collection<Pathway> pathways;

    private final Collection<FeedScopedId> serviceIds;

    private final Map<FeedScopedId, List<ShapePoint>> shapePointsByShapeId;

    private final Map<FeedScopedId, Stop> stopsById;

    private final Map<Trip, List<StopTime>> stopTimesByTrip;

    private final Collection<Transfer> transfers;

    private final Collection<TripPattern> tripPatterns;

    private final Collection<Trip> trips;


    // Lazy initialized indexes

    private Map<Stop, Collection<Stop>> stopsByStation = null;


    /**
     * Create a read only version of the {@link OtpTransitService}.
     *
     * @see OtpTransitServiceBuilder Use builder to create an new OtpTransitDao.
     */
    OtpTransitServiceImpl(OtpTransitServiceBuilder builder) {
        this.agencies = nullSafeUnmodifiableList(builder.getAgencies());
        this.fareAttributes = nullSafeUnmodifiableList(builder.getFareAttributes());
        this.fareRules = nullSafeUnmodifiableList(builder.getFareRules());
        this.feedInfos = nullSafeUnmodifiableList(builder.getFeedInfos());
        this.pathways = nullSafeUnmodifiableList(builder.getPathways());
        this.serviceIds = nullSafeUnmodifiableList(builder.findAllServiceIds());
        this.shapePointsByShapeId = mapShapePoints(builder.getShapePoints());
        this.stopsById = builder.getStops().asImmutableMap();
        this.stopTimesByTrip = builder.getStopTimesSortedByTrip().asImmutableMap();
        this.transfers = nullSafeUnmodifiableList(builder.getTransfers());
        this.tripPatterns = nullSafeUnmodifiableList(builder.getTripPatterns().values());
        this.trips = nullSafeUnmodifiableList(builder.getTrips().values());
    }

    @Override
    public Collection<Agency> getAllAgencies() {
        return agencies;
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
    public Collection<Pathway> getAllPathways() {
        return pathways;
    }

    @Override
    public Collection<FeedScopedId> getAllServiceIds() {
        return serviceIds;
    }

    @Override
    public List<ShapePoint> getShapePointsForShapeId(FeedScopedId shapeId) {
        return nullSafeUnmodifiableList(shapePointsByShapeId.get(shapeId));
    }

    @Override
    public Stop getStopForId(FeedScopedId id) {
        return stopsById.get(id);
    }

    @Override
    public List<Stop> getStopsForStation(Stop station) {
        ensureStopForStations();
        return nullSafeUnmodifiableList(stopsByStation.get(station));
    }

    @Override
    public Collection<Stop> getAllStops() {
        return nullSafeUnmodifiableList(stopsById.values());
    }

    @Override
    public List<StopTime> getStopTimesForTrip(Trip trip) {
        return nullSafeUnmodifiableList(stopTimesByTrip.get(trip));
    }

    @Override
    public Collection<Transfer> getAllTransfers() {
        return transfers;
    }

    @Override
    public Collection<TripPattern> getTripPatterns() {
        return tripPatterns;
    }

    @Override
    public Collection<Trip> getAllTrips() {
        return trips;
    }


    /*  Private Methods */

    private void ensureStopForStations() {
        if (stopsByStation == null) {
            stopsByStation = new HashMap<>();
            for (Stop stop : getAllStops()) {
                if (stop.getLocationType() == 0 && stop.getParentStation() != null) {
                    Stop parentStation = getStopForId(
                            new FeedScopedId(stop.getId().getAgencyId(), stop.getParentStation()));
                    Collection<Stop> subStops = stopsByStation
                            .computeIfAbsent(parentStation, k -> new ArrayList<>(2));
                    subStops.add(stop);
                }
            }
        }
    }

    private Map<FeedScopedId, List<ShapePoint>> mapShapePoints(Collection<ShapePoint> shapePoints) {
        Map<FeedScopedId, List<ShapePoint>> map = shapePoints.stream()
                .collect(groupingBy(ShapePoint::getShapeId));
        for (List<ShapePoint> list : map.values()) {
            Collections.sort(list);
        }
        return map;
    }

    private static <T> List<T> nullSafeUnmodifiableList(Collection<T> c) {
        List<T> list;
        if (c instanceof List) {
            list = (List<T>) c;
        } else {
            list = new ArrayList<>(c);
        }
        return Collections.unmodifiableList(list);
    }
}
