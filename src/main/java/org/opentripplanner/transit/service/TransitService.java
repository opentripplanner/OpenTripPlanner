package org.opentripplanner.transit.service;

import com.google.common.collect.Multimap;
import gnu.trove.set.TIntSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripIdAndServiceDate;
import org.opentripplanner.model.TripOnServiceDate;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitEntity;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Entry point for read-only requests towards the transit API.
 */
public interface TransitService {
  Collection<String> getFeedIds();

  Collection<Agency> getAgencies();

  FeedInfo getFeedInfo(String feedId);

  Collection<Operator> getOperators();

  Multimap<TransitEntity, Notice> getNoticesByElement();

  Collection<Notice> getNoticesByEntity(TransitEntity entity);

  TripPattern getTripPatternForId(FeedScopedId id);

  Collection<TripPattern> getAllTripPatterns();

  Collection<Notice> getNotices();

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

  List<StopTimesInPattern> stopTimesForStop(
    StopLocation stop,
    long startTime,
    int timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancelledTrips
  );

  List<StopTimesInPattern> getStopTimesForStop(
    StopLocation stop,
    LocalDate serviceDate,
    ArrivalDeparture arrivalDeparture
  );

  List<TripTimeOnDate> stopTimesForPatternAtStop(
    StopLocation stop,
    TripPattern pattern,
    long startTime,
    int timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture
  );

  Collection<TripPattern> getPatternsForStop(StopLocation stop, boolean includeRealtimeUpdates);

  Collection<GroupOfRoutes> getGroupsOfRoutes();

  Collection<Route> getRoutesForGroupOfRoutes(GroupOfRoutes groupOfRoutes);

  GroupOfRoutes getGroupOfRoutesForId(FeedScopedId id);

  Timetable getTimetableForTripPattern(TripPattern tripPattern, LocalDate serviceDate);

  TripOnServiceDate getTripOnServiceDateForTripAndDay(FeedScopedId tripId, LocalDate serviceDate);

  TripOnServiceDate getTripOnServiceDateById(FeedScopedId datedServiceJourneyId);

  Map<TripIdAndServiceDate, TripOnServiceDate> getTripOnServiceDateForTripAndDay();

  Map<FeedScopedId, TripOnServiceDate> getTripOnServiceDateById();

  HashSet<TransitMode> getTransitModes();

  Collection<PathTransfer> getTransfersByStop(StopLocation stop);

  TimetableSnapshot getTimetableSnapshot();

  TransitLayer getTransitLayer();

  CalendarService getCalendarService();

  ZoneId getTimeZone();

  TransitAlertService getTransitAlertService();

  FlexIndex getFlexIndex();

  TIntSet getServicesRunningForDate(LocalDate parseString);

  ZonedDateTime getTransitServiceEnds();

  ZonedDateTime getTransitServiceStarts();

  Map<Stop, TransitStopVertex> getStopVertexForStop();

  HashGridSpatialIndex<TransitStopVertex> getStopSpatialIndex();
}
