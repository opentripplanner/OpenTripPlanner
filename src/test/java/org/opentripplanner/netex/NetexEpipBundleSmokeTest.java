package org.opentripplanner.netex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Load a small NeTEx file set without failing. This is just a smoke test and should be excluded
 * from line coverage. The focus of this test is to test that the different parts of the NeTEx works
 * together.
 */
class NetexEpipBundleSmokeTest {

  /**
   * This test load a very simple Netex data set and do assertions on it. For each type we assert
   * some of the most important fields for one element and then check the expected number of that
   * type. This is not a replacement for unit tests on mappers. Try to focus on relation between
   * entities and Netex import integration.
   */
  @Test
  void smokeTestOfNetexEpipLoadData() {
    // Given
    OtpTransitServiceBuilder transitBuilder;
    try (NetexBundle netexBundle = ConstantsForTests.createMinimalNetexEpipBundle()) {
      // Run the check to make sure it does not throw an exception
      netexBundle.checkInputs();

      // When
      transitBuilder = netexBundle.loadBundle(new Deduplicator(), DataImportIssueStore.NOOP);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Then - smoke test model
    OtpTransitService otpModel = transitBuilder.build();

    assertAgencies(otpModel.getAllAgencies());
    assertOperators(otpModel.getAllOperators());
    assertStops(otpModel.stopModel().listRegularStops());
    assertStations(otpModel.stopModel().listStations());
    assertTripPatterns(otpModel.getTripPatterns());
    assertTrips(otpModel.getAllTrips());
    assertServiceIds(otpModel.getAllTrips(), otpModel.getAllServiceIds());

    // And then - smoke test service calendar
    assetServiceCalendar(transitBuilder.buildCalendarServiceData());
  }

  /* private methods */

  private static <T> List<T> list(Collection<T> collection) {
    return new ArrayList<>(collection);
  }

  private static FeedScopedId fId(String id) {
    return new FeedScopedId("HH", id);
  }

  private void assertAgencies(Collection<Agency> agencies) {
    assertEquals(3, agencies.size());
    Agency a = list(agencies).get(0);
    assertEquals("DE::Authority:41::", a.getId().getId());
    assertEquals("HOCHBAHN, Bus", a.getName());
    assertNull(a.getUrl());
    assertEquals("Europe/Berlin", a.getTimezone().toString());
    assertNull(a.getLang());
    assertNull(a.getPhone());
    assertNull(a.getFareUrl());
    assertNull(a.getBrandingUrl());
  }

  private void assertOperators(Collection<Operator> operators) {
    assertEquals(2, operators.size());
    Operator o = list(operators).get(1);
    assertEquals("DE::Operator:66::", o.getId().getId());
    assertEquals("Hamburger Hochbahn AG - Bus", o.getName());
    assertNull(o.getUrl());
    assertEquals("040/19449", o.getPhone());
  }

  private void assertStops(Collection<RegularStop> stops) {
    Map<FeedScopedId, RegularStop> map = stops
      .stream()
      .collect(Collectors.toMap(RegularStop::getId, s -> s));

    RegularStop quay = map.get(fId("DE::Quay:800091_MastM::"));
    assertEquals("Bf. Altona", quay.getName().toString());
    assertEquals(53.5515679274852, quay.getLat(), 0.000001);
    assertEquals(9.93422080466927, quay.getLon(), 0.000001);
    assertEquals("HH:DE::StopPlace:80026_Master::", quay.getParentStation().getId().toString());
    // Assert that quay duplicates are not overriding existing values
    assertEquals(0, quay.getIndex());
    assertNull(quay.getPlatformCode());
    assertEquals(31, stops.size());
  }

  private void assertStations(Collection<Station> stations) {
    Map<FeedScopedId, Station> map = stations
      .stream()
      .collect(Collectors.toMap(Station::getId, s -> s));
    Station station = map.get(fId("DE::StopPlace:80026_Master::"));
    assertEquals("Bf. Altona", station.getName().toString());
    assertEquals(53.5515403864306, station.getLat(), 0.000001);
    assertEquals(9.9342956397828, station.getLon(), 0.000001);
    assertEquals("Europe/Berlin", station.getTimezone().toString());
    assertEquals(3, station.getChildStops().size());
    assertEquals(20, stations.size());
  }

  private void assertTripPatterns(Collection<TripPattern> patterns) {
    Map<FeedScopedId, TripPattern> map = patterns
      .stream()
      .collect(Collectors.toMap(TripPattern::getId, s -> s));
    TripPattern p = map.get(fId("DE::ServiceJourneyPattern:2234991_0::"));
    assertEquals("", p.getTripHeadsign().toString());
    assertEquals("HH", p.getFeedId());
    assertEquals(TransitMode.BUS, p.getMode());
    assertEquals(
      "[RegularStop{HH:DE::Quay:800018_MastM:: Teufelsbrück (Fähre)}, RegularStop{HH:DE::Quay:800091_MastM:: Bf. Altona}]",
      p.getStops().toString()
    );
    List<Trip> trips = p.scheduledTripsAsStream().toList();
    assertEquals("Trip{HH:DE::ServiceJourney:36439031_0:: X86}", trips.get(0).toString());
    assertEquals(55, trips.size());
    assertEquals(4, patterns.size());
  }

  private void assertTrips(Collection<Trip> trips) {
    Map<FeedScopedId, Trip> map = trips.stream().collect(Collectors.toMap(Trip::getId, t -> t));
    Trip t = map.get(fId("DE::ServiceJourney:36439032_0::"));

    assertNotNull(t.getHeadsign());
    assertEquals("", t.getHeadsign().toString());
    assertNull(t.getShortName());
    assertNotNull(t.getServiceId());
    assertEquals("X86", t.getRoute().getName());
    assertEquals(BikeAccess.UNKNOWN, t.getBikesAllowed());
    assertEquals(Accessibility.NO_INFORMATION, t.getWheelchairBoarding());
    assertEquals(110, trips.size());
  }

  private void assertServiceIds(Collection<Trip> trips, Collection<FeedScopedId> serviceIds) {
    Set<FeedScopedId> tripServiceIds = trips
      .stream()
      .map(Trip::getServiceId)
      .collect(Collectors.toSet());
    assertEquals(tripServiceIds, Set.copyOf(serviceIds));
  }

  private void assetServiceCalendar(CalendarServiceData cal) {
    ArrayList<FeedScopedId> sIds = new ArrayList<>(cal.getServiceIds());
    assertEquals(2, sIds.size());
    FeedScopedId serviceId1 = sIds.get(0);

    List<LocalDate> dates = cal.getServiceDatesForServiceId(serviceId1);

    assertEquals("2023-02-02", dates.get(0).toString());
    assertEquals("2023-12-08", dates.get(dates.size() - 1).toString());
    assertEquals(214, dates.size());
  }
}
