package org.opentripplanner.netex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Multimap;
import java.io.Serializable;
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
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.StopTimeKey;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Load a small NeTEx file set without failing. This is just a smoke test and should be excluded
 * from line coverage. The focus of this test is to test that the different parts of the NeTEx works
 * together.
 */
public class NetexEpipBundleSmokeTest {

  /**
   * This test load a very simple Netex data set and do assertions on it. For each type we assert
   * some of the most important fields for one element and then check the expected number of that
   * type. This is not a replacement for unit tests on mappers. Try to focus on relation between
   * entities and Netex import integration.
   */
  @Test
  public void smokeTestOfNetexEpipLoadData() {
    // Given
    NetexBundle netexBundle = ConstantsForTests.createMinimalNetexEpipBundle();

    // Run the check to make sure it does not throw an exception
    netexBundle.checkInputs();

    // When
    OtpTransitServiceBuilder transitBuilder = netexBundle.loadBundle(
      new Deduplicator(),
      DataImportIssueStore.NOOP
    );

    // Then - smoke test model
    OtpTransitService otpModel = transitBuilder.build();

    assertAgencies(otpModel.getAllAgencies());
    assertMultiModalStations(otpModel.stopModel().listMultiModalStations());
    assertOperators(otpModel.getAllOperators());
    assertStops(otpModel.stopModel().listRegularStops());
    assertStations(otpModel.stopModel().listStations());
    assertTripPatterns(otpModel.getTripPatterns());
    assertTrips(otpModel.getAllTrips());
    assertServiceIds(otpModel.getAllTrips(), otpModel.getAllServiceIds());
    assertNoticeAssignments(otpModel.getNoticeAssignments());

    // And then - smoke test service calendar
    assetServiceCalendar(transitBuilder.buildCalendarServiceData());
  }

  /* private methods */

  private static <T> List<T> list(Collection<T> collection) {
    return new ArrayList<>(collection);
  }

  private static StopTimeKey stId(String id, int stopSequenceNr) {
    return StopTimeKey.of(fId(id), stopSequenceNr).build();
  }

  private static FeedScopedId fId(String id) {
    return new FeedScopedId("EN", id);
  }

  private void assertAgencies(Collection<Agency> agencies) {
    assertEquals(2, agencies.size());
    Agency a = list(agencies).get(0);
    assertEquals("DE::Authority:41::", a.getId().getId());
    assertEquals("HOCHBAHN, Bus", a.getName());
    assertNull(a.getUrl());
    assertNull(a.getTimezone());
    assertNull(a.getLang());
    assertNull(a.getPhone());
    assertNull(a.getFareUrl());
    assertNull(a.getBrandingUrl());
  }

  private void assertMultiModalStations(Collection<MultiModalStation> multiModalStations) {
    Map<FeedScopedId, MultiModalStation> map = multiModalStations
      .stream()
      .collect(Collectors.toMap(MultiModalStation::getId, s -> s));
    MultiModalStation multiModalStation = map.get(fId("NSR:StopPlace:58243"));
    assertEquals("Bergkrystallen", multiModalStation.getName().toString());
    assertEquals(59.866603, multiModalStation.getLat(), 0.000001);
    assertEquals(10.821614, multiModalStation.getLon(), 0.000001);
    assertEquals(3, multiModalStations.size());
  }

  private void assertOperators(Collection<Operator> operators) {
    assertEquals(1, operators.size());
    Operator o = list(operators).get(0);
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
    assertEquals("Ankunft", quay.getName().toString());
    assertEquals(53.5515679274852, quay.getLat(), 0.000001);
    assertEquals(9.93422080466927, quay.getLon(), 0.000001);
    assertEquals("DE::StopPlace:80026_Master::", quay.getParentStation().getId().toString());
    assertNull(quay.getPlatformCode());
    assertEquals(4, stops.size());
  }

  private void assertStations(Collection<Station> stations) {
    Map<FeedScopedId, Station> map = stations
      .stream()
      .collect(Collectors.toMap(Station::getId, s -> s));
    Station station = map.get(fId(""));
    assertEquals("Bergkrystallen T", station.getName().toString());
    assertEquals(59.866297, station.getLat(), 0.000001);
    assertEquals(10.821484, station.getLon(), 0.000001);
    assertEquals(5, stations.size());
  }

  private void assertTripPatterns(Collection<TripPattern> patterns) {
    Map<FeedScopedId, TripPattern> map = patterns
      .stream()
      .collect(Collectors.toMap(TripPattern::getId, s -> s));
    TripPattern p = map.get(fId("DE::ServiceJourneyPattern:2234991_0::"));
    assertNull(p.getTripHeadsign());
    assertEquals("HH", p.getFeedId());
    assertEquals(
      "[RegularStop{EN:NSR:Quay:7203 N/A}, RegularStop{EN:NSR:Quay:8027 N/A}]",
      p.getStops().toString()
    );
    assertEquals(
      "[Trip{EN:RUT:ServiceJourney:12-101375-1000 12}]",
      p.scheduledTripsAsStream().toList().toString()
    );

    assertEquals(4, patterns.size());
  }

  private void assertTrips(Collection<Trip> trips) {
    Map<FeedScopedId, Trip> map = trips.stream().collect(Collectors.toMap(Trip::getId, t -> t));
    Trip t = map.get(fId("DE::ServiceJourney:36439032_0::"));

    assertNull(t.getHeadsign());
    assertNull(t.getShortName());
    assertNull(t.getServiceId());
    assertEquals(BikeAccess.UNKNOWN, t.getBikesAllowed());
    assertEquals(Accessibility.NO_INFORMATION, t.getWheelchairBoarding());
    assertEquals(98, trips.size());
  }

  private void assertNoticeAssignments(Multimap<AbstractTransitEntity, Notice> map) {
    assertNote(
      map,
      stId("DE::ServiceJourneyPattern:2234991_0::", 1).getId(),
      "BEHIN",
      "Behindertengerecht"
    );
    assertEquals(32, map.size());
  }

  private void assertNote(
    Multimap<AbstractTransitEntity, Notice> map,
    Serializable entityKey,
    String code,
    String text
  ) {
    AbstractTransitEntity key = map
      .keySet()
      .stream()
      .filter(it -> entityKey.equals(it.getId()))
      .findFirst()
      .orElseThrow(IllegalStateException::new);

    List<Notice> list = list(map.get(key));
    assertNotEquals(
      0,
      list.size(),
      "Notice not found: " + key + " -> <Notice " + code + ", " + text + ">\n\t" + map
    );
    Notice n = list.get(0);
    assertTrue(n.getId().toString().startsWith("DE::Notice:"));
    assertEquals(code, n.publicCode());
    assertEquals(text, n.text());
    assertEquals(8, list.size());
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
    FeedScopedId serviceId2 = sIds.get(1);

    List<LocalDate> dates1 = cal.getServiceDatesForServiceId(serviceId1);
    List<LocalDate> dates2 = cal.getServiceDatesForServiceId(serviceId2);

    assertEquals(dates1, dates2);
    assertEquals("2023-02-02", dates1.get(0).toString());
    assertEquals("2023-12-08", dates1.get(dates1.size() - 1).toString());

    assertEquals(2, cal.getServiceIds().size());
  }
}
