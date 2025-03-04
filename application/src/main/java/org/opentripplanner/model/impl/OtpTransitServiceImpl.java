/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.impl;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.Pathway;
import org.opentripplanner.transit.model.site.PathwayNode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepository;

/**
 * A in-memory implementation of {@link OtpTransitService}. It's super fast for most methods, but
 * only if you have enough memory to load your entire {@link OtpTransitService} into memory.
 * <p>
 * This class is read only, to enforce consistency after generating indexes and ids. You will get an
 * exception if you try to add entities to one of the collections. If you need to modify a {@link
 * OtpTransitService}, you can create a new {@link OtpTransitServiceBuilder} based on your old data,
 * do your modification and create a new unmodifiable instance.
 */
class OtpTransitServiceImpl implements OtpTransitService {

  private final Collection<Agency> agencies;

  private final Collection<Operator> operators;

  private final Collection<FeedInfo> feedInfos;

  private final SiteRepository siteRepository;

  private final ImmutableListMultimap<AbstractTransitEntity, Notice> noticeAssignments;

  private final Collection<Pathway> pathways;

  private final Collection<FeedScopedId> serviceIds;

  private final Map<FeedScopedId, List<ShapePoint>> shapePointsByShapeId;

  private final Map<FeedScopedId, Entrance> entrancesById;

  private final Map<FeedScopedId, PathwayNode> pathwayNodesById;

  private final Map<FeedScopedId, BoardingArea> boardingAreasById;

  private final Map<Trip, List<StopTime>> stopTimesByTrip;

  private final Collection<ConstrainedTransfer> transfers;

  private final Collection<TripPattern> tripPatterns;

  private final Collection<Trip> trips;

  private final Collection<FlexTrip<?, ?>> flexTrips;
  private final Map<FeedScopedId, RegularStop> stopsByScheduledStopPoint;

  /**
   * Create a read only version of the {@link OtpTransitService}.
   */
  OtpTransitServiceImpl(OtpTransitServiceBuilder builder) {
    this.agencies = immutableList(builder.getAgenciesById().values());
    this.feedInfos = immutableList(builder.getFeedInfos());
    this.siteRepository = builder.siteRepository().build();
    this.noticeAssignments = ImmutableListMultimap.copyOf(builder.getNoticeAssignments());
    this.operators = immutableList(builder.getOperatorsById().values());
    this.pathways = immutableList(builder.getPathways());
    this.serviceIds = immutableList(builder.findAllServiceIds());
    this.shapePointsByShapeId = mapShapePoints(builder.getShapePoints());
    this.entrancesById = builder.getEntrances().asImmutableMap();
    this.pathwayNodesById = builder.getPathwayNodes().asImmutableMap();
    this.boardingAreasById = builder.getBoardingAreas().asImmutableMap();
    this.stopTimesByTrip = builder.getStopTimesSortedByTrip().asImmutableMap();
    this.transfers = immutableList(builder.getTransfers());
    this.tripPatterns = immutableList(builder.getTripPatterns().values());
    this.trips = immutableList(builder.getTripsById().values());
    this.flexTrips = immutableList(builder.getFlexTripsById().values());
    this.stopsByScheduledStopPoint = Collections.unmodifiableMap(
      builder.stopsByScheduledStopPoints()
    );
  }

  @Override
  public Collection<Agency> getAllAgencies() {
    return agencies;
  }

  @Override
  public Collection<Operator> getAllOperators() {
    return operators;
  }

  @Override
  public Collection<FeedInfo> getAllFeedInfos() {
    return feedInfos;
  }

  @Override
  public SiteRepository siteRepository() {
    return siteRepository;
  }

  /**
   * Map from Transit Entity(id) to Notices. We need to use Serializable as a common type for ids,
   * since some entities have String, while other have FeedScopeId ids.
   */
  @Override
  public Multimap<AbstractTransitEntity, Notice> getNoticeAssignments() {
    return noticeAssignments;
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
    final List<ShapePoint> points = shapePointsByShapeId.get(shapeId);
    if (points == null) {
      return List.of();
    }
    return immutableList(points);
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
  public Collection<ConstrainedTransfer> getAllTransfers() {
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

  @Override
  public Collection<FlexTrip<?, ?>> getAllFlexTrips() {
    return flexTrips;
  }

  @Override
  public boolean hasActiveTransit() {
    return serviceIds.size() > 0;
  }

  /**
   * @see org.opentripplanner.transit.service.TimetableRepository#findStopByScheduledStopPoint(FeedScopedId)
   */
  @Override
  public Map<FeedScopedId, RegularStop> stopsByScheduledStopPoint() {
    return stopsByScheduledStopPoint;
  }

  /*  Private Methods */

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

  private Map<FeedScopedId, List<ShapePoint>> mapShapePoints(
    Multimap<FeedScopedId, ShapePoint> shapePoints
  ) {
    Map<FeedScopedId, List<ShapePoint>> map = new HashMap<>();
    for (Map.Entry<FeedScopedId, Collection<ShapePoint>> entry : shapePoints.asMap().entrySet()) {
      map.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    for (List<ShapePoint> list : map.values()) {
      Collections.sort(list);
    }
    return map;
  }
}
