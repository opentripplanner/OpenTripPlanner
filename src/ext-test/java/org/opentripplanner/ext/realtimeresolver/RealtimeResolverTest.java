package org.opentripplanner.ext.realtimeresolver;

import static org.junit.jupiter.api.Assertions.*;

import gnu.trove.set.TIntSet;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.legreference.LegReference;
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
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.GraphUpdaterStatus;

class RealtimeResolverTest {

  private static MockLeg MOCK_LEG = new MockLeg();

  private static class MockLeg implements Leg {

    @Override
    public boolean isTransitLeg() {
      return true;
    }

    @Override
    public boolean hasSameMode(Leg other) {
      return false;
    }

    @Override
    public ZonedDateTime getStartTime() {
      return ZonedDateTime.now();
    }

    @Override
    public ZonedDateTime getEndTime() {
      return ZonedDateTime.now();
    }

    @Override
    public double getDistanceMeters() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public Place getFrom() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public Place getTo() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public LineString getLegGeometry() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public int getGeneralizedCost() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public LegReference getLegReference() {
      return new MockLegReference();
    }
  }

  private static class MockLegReference implements LegReference {

    @Override
    public Leg getLeg(TransitService transitService) {
      return MOCK_LEG;
    }
  }

  private static class MockTransitService implements TransitService {

    @Override
    public Collection<String> getFeedIds() {
      return null;
    }

    @Override
    public Collection<Agency> getAgencies() {
      return null;
    }

    @Override
    public FeedInfo getFeedInfo(String feedId) {
      return null;
    }

    @Override
    public Collection<Operator> getOperators() {
      return null;
    }

    @Override
    public Collection<Notice> getNoticesByEntity(AbstractTransitEntity<?, ?> entity) {
      return null;
    }

    @Override
    public TripPattern getTripPatternForId(FeedScopedId id) {
      return null;
    }

    @Override
    public Collection<TripPattern> getAllTripPatterns() {
      return null;
    }

    @Override
    public Collection<Notice> getNotices() {
      return null;
    }

    @Override
    public Station getStationById(FeedScopedId id) {
      return null;
    }

    @Override
    public MultiModalStation getMultiModalStation(FeedScopedId id) {
      return null;
    }

    @Override
    public Collection<Station> getStations() {
      return null;
    }

    @Override
    public Integer getServiceCodeForId(FeedScopedId id) {
      return null;
    }

    @Override
    public TIntSet getServiceCodesRunningForDate(LocalDate date) {
      return null;
    }

    @Override
    public Agency getAgencyForId(FeedScopedId id) {
      return null;
    }

    @Override
    public Route getRouteForId(FeedScopedId id) {
      return null;
    }

    @Override
    public Set<Route> getRoutesForStop(StopLocation stop) {
      return null;
    }

    @Override
    public Collection<TripPattern> getPatternsForStop(StopLocation stop) {
      return null;
    }

    @Override
    public Collection<TripPattern> getPatternsForStop(
      StopLocation stop,
      boolean includeRealtimeUpdates
    ) {
      return null;
    }

    @Override
    public Collection<Trip> getTripsForStop(StopLocation stop) {
      return null;
    }

    @Override
    public Collection<Operator> getAllOperators() {
      return null;
    }

    @Override
    public Operator getOperatorForId(FeedScopedId id) {
      return null;
    }

    @Override
    public RegularStop getRegularStop(FeedScopedId id) {
      return null;
    }

    @Override
    public Collection<StopLocation> listStopLocations() {
      return null;
    }

    @Override
    public Collection<RegularStop> listRegularStops() {
      return null;
    }

    @Override
    public StopLocation getStopLocation(FeedScopedId parseId) {
      return null;
    }

    @Override
    public Collection<StopLocationsGroup> listStopLocationGroups() {
      return null;
    }

    @Override
    public StopLocationsGroup getStopLocationsGroup(FeedScopedId id) {
      return null;
    }

    @Override
    public AreaStop getAreaStop(FeedScopedId id) {
      return null;
    }

    @Override
    public Trip getTripForId(FeedScopedId id) {
      return null;
    }

    @Override
    public Collection<Trip> getAllTrips() {
      return null;
    }

    @Override
    public Collection<Route> getAllRoutes() {
      return null;
    }

    @Override
    public TripPattern getPatternForTrip(Trip trip) {
      return null;
    }

    @Override
    public TripPattern getPatternForTrip(Trip trip, LocalDate serviceDate) {
      return null;
    }

    @Override
    public Collection<TripPattern> getPatternsForRoute(Route route) {
      return null;
    }

    @Override
    public MultiModalStation getMultiModalStationForStation(Station station) {
      return null;
    }

    @Override
    public List<StopTimesInPattern> stopTimesForStop(
      StopLocation stop,
      Instant startTime,
      Duration timeRange,
      int numberOfDepartures,
      ArrivalDeparture arrivalDeparture,
      boolean includeCancelledTrips
    ) {
      return null;
    }

    @Override
    public List<StopTimesInPattern> getStopTimesForStop(
      StopLocation stop,
      LocalDate serviceDate,
      ArrivalDeparture arrivalDeparture,
      boolean includeCancellations
    ) {
      return null;
    }

    @Override
    public List<TripTimeOnDate> stopTimesForPatternAtStop(
      StopLocation stop,
      TripPattern pattern,
      Instant startTime,
      Duration timeRange,
      int numberOfDepartures,
      ArrivalDeparture arrivalDeparture,
      boolean includeCancellations
    ) {
      return null;
    }

    @Override
    public Collection<GroupOfRoutes> getGroupsOfRoutes() {
      return null;
    }

    @Override
    public Collection<Route> getRoutesForGroupOfRoutes(GroupOfRoutes groupOfRoutes) {
      return null;
    }

    @Override
    public GroupOfRoutes getGroupOfRoutesForId(FeedScopedId id) {
      return null;
    }

    @Override
    public Timetable getTimetableForTripPattern(TripPattern tripPattern, LocalDate serviceDate) {
      return null;
    }

    @Override
    public TripPattern getRealtimeAddedTripPattern(FeedScopedId tripId, LocalDate serviceDate) {
      return null;
    }

    @Override
    public boolean hasRealtimeAddedTripPatterns() {
      return false;
    }

    @Override
    public TripOnServiceDate getTripOnServiceDateForTripAndDay(
      TripIdAndServiceDate tripIdAndServiceDate
    ) {
      return null;
    }

    @Override
    public TripOnServiceDate getTripOnServiceDateById(FeedScopedId datedServiceJourneyId) {
      return null;
    }

    @Override
    public Collection<TripOnServiceDate> getAllTripOnServiceDates() {
      return null;
    }

    @Override
    public Set<TransitMode> getTransitModes() {
      return null;
    }

    @Override
    public Collection<PathTransfer> getTransfersByStop(StopLocation stop) {
      return null;
    }

    @Override
    public TransitLayer getTransitLayer() {
      return null;
    }

    @Override
    public TransitLayer getRealtimeTransitLayer() {
      return null;
    }

    @Override
    public CalendarService getCalendarService() {
      return null;
    }

    @Override
    public ZoneId getTimeZone() {
      return null;
    }

    @Override
    public TransitAlertService getTransitAlertService() {
      return null;
    }

    @Override
    public FlexIndex getFlexIndex() {
      return null;
    }

    @Override
    public ZonedDateTime getTransitServiceEnds() {
      return null;
    }

    @Override
    public ZonedDateTime getTransitServiceStarts() {
      return null;
    }

    @Override
    public Optional<Coordinate> getCenter() {
      return Optional.empty();
    }

    @Override
    public TransferService getTransferService() {
      return null;
    }

    @Override
    public boolean transitFeedCovers(Instant dateTime) {
      return false;
    }

    @Override
    public Collection<RegularStop> findRegularStop(Envelope envelope) {
      return null;
    }

    @Override
    public GraphUpdaterStatus getUpdaterStatus() {
      return null;
    }
  }

  @Test
  void testPopulateLegsWithRealtime() {
    var legs = new ArrayList<Leg>();
    legs.add(new MockLeg());
    legs.add(new MockLeg());
    legs.add(new MockLeg());

    var itineraries = new ArrayList<Itinerary>();
    itineraries.add(new Itinerary(legs));
    itineraries.add(new Itinerary(legs));
    itineraries.add(new Itinerary(legs));

    RealtimeResolver.populateLegsWithRealtime(itineraries, new MockTransitService());

    assertEquals(3, itineraries.size());

    itineraries.forEach(it -> {
      var lgs = it.getLegs();
      assertEquals(3, lgs.size());
      lgs.forEach(l -> assertEquals(MOCK_LEG, l));
    });
  }
}
