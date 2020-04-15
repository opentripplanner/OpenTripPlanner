/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.impl;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.BoardingArea;
import org.opentripplanner.model.Entrance;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GroupOfStations;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 */
class OtpTransitServiceImpl implements OtpTransitService {

    private static final Logger LOG = LoggerFactory.getLogger(OtpTransitService.class);

    private final Collection<Agency> agencies;

    private final Collection<Operator> operators;

    private final Collection<FareAttribute> fareAttributes;

    private final Collection<FareRule> fareRules;

    private final Collection<FeedInfo> feedInfos;

    private final Collection<GroupOfStations> groupsOfStations;

    private final Collection<MultiModalStation> multiModalStations;

    private final ImmutableListMultimap<TransitEntity<?>, Notice> noticeAssignments;

    private final Collection<Pathway> pathways;

    private final Collection<FeedScopedId> serviceIds;

    private final Map<FeedScopedId, List<ShapePoint>> shapePointsByShapeId;

    private final Map<FeedScopedId, Station> stationsById;

    private final Map<FeedScopedId, Stop> stopsById;

    private final Map<FeedScopedId, Entrance> entrancesById;

    private final Map<FeedScopedId, PathwayNode> pathwayNodesById;

    private final Map<FeedScopedId, BoardingArea> boardingAreasById;

    private final Map<Trip, List<StopTime>> stopTimesByTrip;

    private final Collection<Transfer> transfers;

    private final Collection<TripPattern> tripPatterns;

    private final Collection<Trip> trips;

    /**
     * Create a read only version of the {@link OtpTransitService}.
     */
    OtpTransitServiceImpl(OtpTransitServiceBuilder builder) {
        this.agencies = immutableList(builder.getAgenciesById().values());
        this.fareAttributes = immutableList(builder.getFareAttributes());
        this.fareRules = immutableList(builder.getFareRules());
        this.feedInfos = immutableList(builder.getFeedInfos());
        this.groupsOfStations = builder.getGroupsOfStationsById().values();
        this.multiModalStations = builder.getMultiModalStationsById().values();
        this.noticeAssignments = ImmutableListMultimap.copyOf(builder.getNoticeAssignments());
        this.operators = immutableList(builder.getOperatorsById().values());
        this.pathways = immutableList(builder.getPathways());
        this.serviceIds = immutableList(builder.findAllServiceIds());
        this.shapePointsByShapeId = mapShapePoints(builder.getShapePoints());
        this.stationsById = builder.getStations().asImmutableMap();
        this.stopsById = builder.getStops().asImmutableMap();
        this.entrancesById = builder.getEntrances().asImmutableMap();
        this.pathwayNodesById = builder.getPathwayNodes().asImmutableMap();
        this.boardingAreasById = builder.getBoardingAreas().asImmutableMap();
        this.stopTimesByTrip = builder.getStopTimesSortedByTrip().asImmutableMap();
        this.transfers = immutableList(builder.getTransfers());
        this.tripPatterns = immutableList(builder.getTripPatterns().values());
        this.trips = immutableList(builder.getTripsById().values());
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
    public Collection<GroupOfStations> getAllGroupsOfStations() {
        return immutableList(groupsOfStations);
    }

    @Override
    public Collection<MultiModalStation> getAllMultiModalStations() {
        return immutableList(multiModalStations);
    }

    /**
     * Map from Transit Entity(id) to Notices. We need to use Serializable as a common type
     * for ids, since some entities have String, while other have FeedScopeId ids.
     */
    @Override
    public Multimap<TransitEntity<?>, Notice> getNoticeAssignments() {
        return noticeAssignments;
    }

    @Override
    public Collection<Pathway> getAllPathways() {
        return pathways;
    }

    @Override
    public Collection<Operator> getAllOperators() {
        return operators;
    }

    @Override
    public Collection<FeedScopedId> getAllServiceIds() {
        return serviceIds;
    }

    @Override
    public List<ShapePoint> getShapePointsForShapeId(FeedScopedId shapeId) {
        return immutableList(shapePointsByShapeId.get(shapeId));
    }

    @Override
    public Station getStationForId(FeedScopedId id) {
        return stationsById.get(id);
    }

    @Override
    public Stop getStopForId(FeedScopedId id) {
        return stopsById.get(id);
    }

    @Override
    public Collection<Station> getAllStations() {
        return immutableList(stationsById.values());
    }

    @Override
    public Collection<Stop> getAllStops() {
        return immutableList(stopsById.values());
    }

    @Override
    public Collection<Entrance> getAllEntrances() {
        return immutableList(entrancesById.values());
    }

    @Override
    public Collection<PathwayNode> getAllPathwayNodes() {
        return immutableList(pathwayNodesById.values());
    }

    @Override
    public Collection<BoardingArea> getAllBoardingAreas() {
        return immutableList(boardingAreasById.values());
    }

    @Override
    public List<StopTime> getStopTimesForTrip(Trip trip) {
        return immutableList(stopTimesByTrip.get(trip));
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

    private Map<FeedScopedId, List<ShapePoint>> mapShapePoints(
        Multimap<FeedScopedId, ShapePoint> shapePoints
    ) {
        Map<FeedScopedId, List<ShapePoint>> map = new HashMap<>();
        for (Map.Entry<FeedScopedId, Collection<ShapePoint>> entry
            : shapePoints.asMap().entrySet()) {
            map.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        for (List<ShapePoint> list : map.values()) {
            Collections.sort(list);
        }
        return map;
    }

    /**
     * Convert the given collection into a new immutable List.
     */
    private static <T> List<T> immutableList(Collection<T> c) {
        List<T> list;
        if (c instanceof List) {
            list = (List<T>) c;
        } else {
            list = new ArrayList<>(c);
        }
        return Collections.unmodifiableList(list);
    }

}
