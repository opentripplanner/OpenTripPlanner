package org.opentripplanner.transit.service;

import gnu.trove.set.TIntSet;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.updater.GraphUpdaterStatus;

/**
 * Entry point for read-only requests towards the transit API.
 */
public interface TransitService {
  Collection<String> getFeedIds();

  Collection<Agency> getAgencies();
  Optional<Agency> findAgencyById(FeedScopedId id);

  FeedInfo getFeedInfo(String feedId);

  Collection<Operator> getOperators();

  Collection<Notice> getNoticesByEntity(AbstractTransitEntity<?, ?> entity);

  TripPattern getTripPatternForId(FeedScopedId id);

  Collection<TripPattern> getAllTripPatterns();

  Collection<Notice> getNotices();

  Station getStationById(FeedScopedId id);

  MultiModalStation getMultiModalStation(FeedScopedId id);

  Collection<Station> getStations();

  Integer getServiceCodeForId(FeedScopedId id);

  TIntSet getServiceCodesRunningForDate(LocalDate date);

  Agency getAgencyForId(FeedScopedId id);

  Route getRouteForId(FeedScopedId id);

  Set<Route> getRoutesForStop(StopLocation stop);

  Collection<TripPattern> getPatternsForStop(StopLocation stop);

  Collection<TripPattern> getPatternsForStop(StopLocation stop, boolean includeRealtimeUpdates);

  Collection<Trip> getTripsForStop(StopLocation stop);

  Collection<Operator> getAllOperators();

  Operator getOperatorForId(FeedScopedId id);

  RegularStop getRegularStop(FeedScopedId id);

  Collection<StopLocation> listStopLocations();

  Collection<RegularStop> listRegularStops();

  Collection<GroupStop> listGroupStops();

  StopLocation getStopLocation(FeedScopedId parseId);

  Collection<StopLocation> getStopOrChildStops(FeedScopedId id);

  Collection<StopLocationsGroup> listStopLocationGroups();

  StopLocationsGroup getStopLocationsGroup(FeedScopedId id);

  AreaStop getAreaStop(FeedScopedId id);

  Trip getTripForId(FeedScopedId id);

  Collection<Trip> getAllTrips();

  Collection<Route> getAllRoutes();

  TripPattern getPatternForTrip(Trip trip);

  TripPattern getPatternForTrip(Trip trip, LocalDate serviceDate);

  Collection<TripPattern> getPatternsForRoute(Route route);

  MultiModalStation getMultiModalStationForStation(Station station);

  List<StopTimesInPattern> stopTimesForStop(
    StopLocation stop,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancelledTrips
  );

  List<StopTimesInPattern> getStopTimesForStop(
    StopLocation stop,
    LocalDate serviceDate,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  );

  List<TripTimeOnDate> stopTimesForPatternAtStop(
    StopLocation stop,
    TripPattern pattern,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  );

  Collection<GroupOfRoutes> getGroupsOfRoutes();

  Collection<Route> getRoutesForGroupOfRoutes(GroupOfRoutes groupOfRoutes);

  GroupOfRoutes getGroupOfRoutesForId(FeedScopedId id);

  Timetable getTimetableForTripPattern(TripPattern tripPattern, LocalDate serviceDate);

  TripPattern getRealtimeAddedTripPattern(FeedScopedId tripId, LocalDate serviceDate);

  boolean hasRealtimeAddedTripPatterns();

  TripOnServiceDate getTripOnServiceDateForTripAndDay(TripIdAndServiceDate tripIdAndServiceDate);

  TripOnServiceDate getTripOnServiceDateById(FeedScopedId datedServiceJourneyId);

  Collection<TripOnServiceDate> getAllTripOnServiceDates();

  Set<TransitMode> getTransitModes();

  Collection<PathTransfer> getTransfersByStop(StopLocation stop);

  TransitLayer getTransitLayer();

  TransitLayer getRealtimeTransitLayer();

  CalendarService getCalendarService();

  ZoneId getTimeZone();

  TransitAlertService getTransitAlertService();

  FlexIndex getFlexIndex();

  ZonedDateTime getTransitServiceEnds();

  ZonedDateTime getTransitServiceStarts();

  TransferService getTransferService();

  boolean transitFeedCovers(Instant dateTime);

  Collection<RegularStop> findRegularStops(Envelope envelope);

  Collection<AreaStop> findAreaStops(Envelope envelope);

  GraphUpdaterStatus getUpdaterStatus();

  /**
   * For a {@link StopLocationsGroup} get all child stops and get their modes.
   * <p>
   * The mode is either taken from {@link StopLocation#getGtfsVehicleType()} (if non-null)
   * or from the list of patterns that use the stop location.
   * <p>
   * The returning stream is ordered by the number of occurrences of the mode in the child stops.
   * So, if more patterns of mode BUS than RAIL visit the group, the result will be [BUS,RAIL].
   */
  List<TransitMode> getModesOfStopLocationsGroup(StopLocationsGroup station);
  /**
   * For a {@link StopLocation} return its modes.
   * <p>
   * The mode is either taken from {@link StopLocation#getGtfsVehicleType()} (if non-null)
   * or from the list of patterns that use the stop location.
   * <p>
   * If {@link StopLocation#getGtfsVehicleType()} is null the returning stream is ordered by the number
   * of occurrences of the mode in the stop.
   * <p>
   * So, if more patterns of mode BUS than RAIL visit the stop, the result will be [BUS,RAIL].
   */
  List<TransitMode> getModesOfStopLocation(StopLocation stop);
}
