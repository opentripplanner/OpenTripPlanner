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
import org.opentripplanner.transit.model.framework.Deduplicator;
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
 * TransitService is a read-only interface for retrieving public transport data. It provides a
 * frozen view of all these elements at a point in time, which is not affected by incoming realtime
 * data, allowing results to remain stable over the course of a request. This can be used for
 * fetching tables of specific information like the routes passing through a particular stop, or for
 * gaining access to the entirety of the data to perform routing.
 * <p>
 * TODO RT_AB: this interface seems to provide direct access to TransitLayer but not TransitModel.
 *   Is this intentional, because TransitLayer is meant to be read-only and TransitModel is not?
 *   Should this be renamed TransitDataService since it seems to provide access to the data but
 *   not to transit routing functionality (which is provided by the RoutingService)?
 *   The DefaultTransitService implementation has a TransitModel instance and many of its methods
 *   read through to that TransitModel instance. But that field itself is not exposed, while the
 *   TransitLayer is here. It seems like exposing the raw TransitLayer is still a risk since it's
 *   copy-on-write and shares a lot of objects with any other TransitLayer instances.
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

  /**
   * Return all the scheduled trip patterns for a specific stop
   * (not taking into account real-time updates).
   */
  Collection<TripPattern> getPatternsForStop(StopLocation stop);

  /**
   * Returns all the patterns for a specific stop. If includeRealtimeUpdates is set, new patterns
   * added by realtime updates are added to the collection.
   */
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

  /**
   * Return the scheduled trip pattern for a given trip (not taking into account real-time updates)
   */
  TripPattern getPatternForTrip(Trip trip);

  /**
   * Return the trip pattern for a given trip on a service date. The real-time updated version
   * is returned if it exists, otherwise the scheduled trip pattern is returned.
   *
   */
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

  /**
   * Return the timetable for a given trip pattern and date, taking into account real-time updates.
   * If no real-times update are applied, fall back to scheduled data.
   */
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

  Deduplicator getDeduplicator();
}
