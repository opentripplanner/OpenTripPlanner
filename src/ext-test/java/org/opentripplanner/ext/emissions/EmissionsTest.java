package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.model.Grams;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

class EmissionsTest {

  private DefaultEmissionsService eService;
  private EmissionsFilter emissionsFilter;

  static final ZonedDateTime TIME = OffsetDateTime
    .parse("2023-07-20T17:49:06+03:00")
    .toZonedDateTime();

  private static final Agency subject = Agency
    .of(TransitModelForTest.id("F:1"))
    .withName("Foo_CO")
    .withTimezone("Europe/Helsinki")
    .build();

  @BeforeEach
  void SetUp() {
    Map<FeedScopedId, Double> emissions = new HashMap<>();
    emissions.put(new FeedScopedId("F", "1"), (0.12 / 12));
    emissions.put(new FeedScopedId("F", "2"), 0.0);
    EmissionsDataModel emissionsDataModel = new EmissionsDataModel(emissions, 0.131);
    this.eService = new DefaultEmissionsService(emissionsDataModel);
    this.emissionsFilter = new EmissionsFilter(eService);
  }

  @Test
  void testGetEmissionsForItinerary() {
    var testModel = TransitModelForTest.of();
    var stopOne = testModel.stop("1:stop1", 60, 25).build();
    var stopTwo = testModel.stop("1:stop1", 61, 25).build();
    var stopThree = testModel.stop("1:stop1", 62, 25).build();
    var stopPattern = TransitModelForTest.stopPattern(stopOne, stopTwo, stopThree);
    var route = TransitModelForTest.route(id("1")).build();
    var pattern = TransitModelForTest.tripPattern("1", route).withStopPattern(stopPattern).build();
    var stoptime = new StopTime();
    var stoptimes = new ArrayList<StopTime>();
    stoptimes.add(stoptime);
    var trip = Trip
      .of(FeedScopedId.parse("FOO:BAR"))
      .withMode(TransitMode.BUS)
      .withRoute(route)
      .build();
    var leg = new ScheduledTransitLegBuilder<>()
      .withTripTimes(new TripTimes(trip, stoptimes, new Deduplicator()))
      .withTripPattern(pattern)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(2)
      .withStartTime(TIME)
      .withEndTime(TIME.plusMinutes(10))
      .withServiceDate(TIME.toLocalDate())
      .withZoneId(ZoneIds.BERLIN)
      .build();
    Itinerary i = new Itinerary(List.of(leg));
    assertEquals(
      new Grams(2223.902),
      emissionsFilter.filter(List.of(i)).get(0).getEmissionsPerPerson().getCo2()
    );
  }

  @Test
  void testGetEmissionsForCarRoute() {
    var leg = StreetLeg
      .create()
      .withMode(TraverseMode.CAR)
      .withDistanceMeters(214.4)
      .withStartTime(TIME)
      .withEndTime(TIME.plusHours(1))
      .build();
    Itinerary i = new Itinerary(List.of(leg));
    assertEquals(
      new Grams(28.0864),
      emissionsFilter.filter(List.of(i)).get(0).getEmissionsPerPerson().getCo2()
    );
  }

  @Test
  void testNoEmissionsForFeedWithoutEmissionsConfigured() {
    var testModel = TransitModelForTest.of();
    Map<FeedScopedId, Double> emissions = new HashMap<>();
    emissions.put(new FeedScopedId("G", "1"), (0.12 / 12));
    EmissionsDataModel emissionsDataModel = new EmissionsDataModel(emissions, 0.131);

    this.eService = new DefaultEmissionsService(emissionsDataModel);
    this.emissionsFilter = new EmissionsFilter(this.eService);

    var route = TransitModelForTest.route(id("1")).withAgency(subject).build();
    var pattern = TransitModelForTest
      .tripPattern("1", route)
      .withStopPattern(testModel.stopPattern(3))
      .build();
    var stoptime = new StopTime();
    var stoptimes = new ArrayList<StopTime>();
    stoptimes.add(stoptime);
    var trip = Trip
      .of(FeedScopedId.parse("FOO:BAR"))
      .withMode(TransitMode.BUS)
      .withRoute(route)
      .build();
    var leg = new ScheduledTransitLegBuilder<>()
      .withTripTimes(new TripTimes(trip, stoptimes, new Deduplicator()))
      .withTripPattern(pattern)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(2)
      .withStartTime(TIME)
      .withEndTime(TIME.plusMinutes(10))
      .withServiceDate(TIME.toLocalDate())
      .withZoneId(ZoneIds.BERLIN)
      .build();
    Itinerary i = new Itinerary(List.of(leg));
    assertEquals(null, emissionsFilter.filter(List.of(i)).get(0).getEmissionsPerPerson());
  }

  @Test
  void testZeroEmissionsForItineraryWithZeroEmissions() {
    var testModel = TransitModelForTest.of();
    var stopOne = testModel.stop("1:stop1", 60, 25).build();
    var stopTwo = testModel.stop("1:stop1", 61, 25).build();
    var stopThree = testModel.stop("1:stop1", 62, 25).build();
    var stopPattern = TransitModelForTest.stopPattern(stopOne, stopTwo, stopThree);
    var route = TransitModelForTest.route(id("2")).build();
    var pattern = TransitModelForTest.tripPattern("1", route).withStopPattern(stopPattern).build();
    var stoptime = new StopTime();
    var stoptimes = new ArrayList<StopTime>();
    stoptimes.add(stoptime);
    var trip = Trip
      .of(FeedScopedId.parse("FOO:BAR"))
      .withMode(TransitMode.BUS)
      .withRoute(route)
      .build();
    var leg = new ScheduledTransitLegBuilder<>()
      .withTripTimes(new TripTimes(trip, stoptimes, new Deduplicator()))
      .withTripPattern(pattern)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(2)
      .withStartTime(TIME)
      .withEndTime(TIME.plusMinutes(10))
      .withServiceDate(TIME.toLocalDate())
      .withZoneId(ZoneIds.BERLIN)
      .build();
    Itinerary i = new Itinerary(List.of(leg));
    assertEquals(
      new Grams(0.0),
      emissionsFilter.filter(List.of(i)).get(0).getEmissionsPerPerson().getCo2()
    );
  }
}
