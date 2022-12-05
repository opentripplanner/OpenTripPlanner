package org.opentripplanner.netex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
public class NetexBundleSmokeTest {

  /**
   * This test load a very simple Netex data set and do assertions on it. For each type we assert
   * some of the most important fields for one element and then check the expected number of that
   * type. This is not a replacement for unit tests on mappers. Try to focus on relation between
   * entities and Netex import integration.
   */
  @Test
  public void smokeTestOfNetexLoadData() {
    // Given
    NetexBundle netexBundle = ConstantsForTests.createMinimalNetexBundle();

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
    assertEquals(1, agencies.size());
    Agency a = list(agencies).get(0);
    assertEquals("RUT:Authority:RUT", a.getId().getId());
    assertEquals("RUT", a.getName());
    assertNull(a.getUrl());
    assertEquals("Europe/Oslo", a.getTimezone().getId());
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
    assertEquals("RUT:Operator:130c", o.getId().getId());
    assertEquals("Ruter", o.getName());
    assertNull(o.getUrl());
    assertNull(o.getPhone());
  }

  private void assertStops(Collection<RegularStop> stops) {
    Map<FeedScopedId, RegularStop> map = stops
      .stream()
      .collect(Collectors.toMap(RegularStop::getId, s -> s));

    RegularStop quay = map.get(fId("NSR:Quay:122003"));
    assertEquals("N/A", quay.getName().toString());
    assertEquals(59.909803, quay.getLat(), 0.000001);
    assertEquals(10.748062, quay.getLon(), 0.000001);
    assertEquals("EN:NSR:StopPlace:3995", quay.getParentStation().getId().toString());
    assertEquals("L", quay.getPlatformCode());
    assertEquals(16, stops.size());
  }

  private void assertStations(Collection<Station> stations) {
    Map<FeedScopedId, Station> map = stations
      .stream()
      .collect(Collectors.toMap(Station::getId, s -> s));
    Station station = map.get(fId("NSR:StopPlace:5825"));
    assertEquals("Bergkrystallen T", station.getName().toString());
    assertEquals(59.866297, station.getLat(), 0.000001);
    assertEquals(10.821484, station.getLon(), 0.000001);
    assertEquals(5, stations.size());
  }

  private void assertTripPatterns(Collection<TripPattern> patterns) {
    Map<FeedScopedId, TripPattern> map = patterns
      .stream()
      .collect(Collectors.toMap(TripPattern::getId, s -> s));
    TripPattern p = map.get(fId("RUT:JourneyPattern:12-1"));
    assertEquals("Jernbanetorget", p.getTripHeadsign().toString());
    assertEquals("EN", p.getFeedId());
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
    Trip t = map.get(fId("RUT:ServiceJourney:12-101375-1001"));

    assertEquals("Jernbanetorget", t.getHeadsign().toString());
    assertNull(t.getShortName());
    assertNotNull(t.getServiceId());
    assertEquals("Ruter", t.getOperator().getName());
    assertEquals(BikeAccess.UNKNOWN, t.getBikesAllowed());
    assertEquals(Accessibility.NO_INFORMATION, t.getWheelchairBoarding());
    assertEquals(4, trips.size());
  }

  private void assertNoticeAssignments(Multimap<AbstractTransitEntity, Notice> map) {
    assertNote(map, fId("RUT:ServiceJourney:4-101468-583"), "045", "Notice on ServiceJourney");
    assertNote(
      map,
      stId("RUT:ServiceJourney:4-101468-583", 0).getId(),
      "035",
      "Notice on TimetabledPassingTime"
    );
    assertNote(map, fId("RUT:Line:4"), "075", "Notice on Line");
    assertNote(
      map,
      stId("RUT:ServiceJourney:4-101493-1098", 1).getId(),
      "090",
      "Notice on Journeypattern"
    );
    assertEquals(4, map.size());
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
    assertTrue(n.getId().toString().startsWith("EN:RUT:Notice:"));
    assertEquals(code, n.publicCode());
    assertEquals(text, n.text());
    assertEquals(1, list.size());
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

    if (dates1.size() > dates2.size()) {
      var datesTemp = dates1;
      dates1 = dates2;
      dates2 = datesTemp;
    }

    assertEquals(
      "[2017-12-21, 2017-12-22, 2017-12-25, 2017-12-26, 2017-12-27, 2017-12-28, " +
      "2017-12-29, 2018-01-02, 2018-01-03, 2018-01-04]",
      dates1.toString()
    );
    assertEquals(
      "[2017-12-21, 2017-12-22, 2017-12-23, 2017-12-24, 2017-12-25, 2017-12-26, " +
      "2017-12-27, 2017-12-28, 2017-12-29, 2017-12-30, 2017-12-31, 2018-01-02, " +
      "2018-01-03, 2018-01-04]",
      dates2.toString()
    );
    assertEquals(2, cal.getServiceIds().size());
  }
}
