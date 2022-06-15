package org.opentripplanner.transit.service;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.timetable.Trip;

public interface TransitService {
  Collection<String> getFeedIds();

  Collection<Agency> getAgencies();

  FeedInfo getFeedInfo(String feedId);

  Collection<Operator> getOperators();

  Multimap<TransitEntity, Notice> getNoticesByElement();

  Collection<Notice> getNoticesByEntity(TransitEntity entity);

  TripPattern getTripPatternForId(FeedScopedId id);

  Collection<TripPattern> getTripPatterns();

  Collection<Notice> getNotices();

  Collection<StopLocation> getStopsByBoundingBox(
    double minLat,
    double minLon,
    double maxLat,
    double maxLon
  );

  List<T2<Stop, Double>> getStopsInRadius(WgsCoordinate center, double radius);

  Station getStationById(FeedScopedId id);

  MultiModalStation getMultiModalStation(FeedScopedId id);

  Collection<Station> getStations();

  Map<FeedScopedId, Integer> getServiceCodes();

  FlexStopLocation getLocationById(FeedScopedId id);

  Set<StopLocation> getAllFlexStopsFlat();

  Agency getAgencyForId(FeedScopedId id);

  StopLocation getStopForId(FeedScopedId id);

  Route getRouteForId(FeedScopedId id);

  Set<Route> getRoutesForStop(StopLocation stop);

  Collection<TripPattern> getPatternsForStop(StopLocation stop);

  Collection<TripPattern> getPatternsForStop(
    StopLocation stop,
    TimetableSnapshot timetableSnapshot
  );

  Collection<Operator> getAllOperators();

  Map<FeedScopedId, Operator> getOperatorForId();

  Collection<StopLocation> getAllStops();

  Map<FeedScopedId, Trip> getTripForId();

  Collection<Route> getAllRoutes();

  Map<Trip, TripPattern> getPatternForTrip();

  Multimap<String, TripPattern> getPatternsForFeedId();

  Multimap<Route, TripPattern> getPatternsForRoute();

  Map<Station, MultiModalStation> getMultiModalStationForStations();
}
