package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.Grams;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.LegConstructionSupport;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class EmissionsTest {

  private static DefaultEmissionsService eService;
  private static DecorateWithEmission decorateWithEmission;

  static final ZonedDateTime TIME = OffsetDateTime.parse(
    "2023-07-20T17:49:06+03:00"
  ).toZonedDateTime();

  private static final StreetLeg STREET_LEG = StreetLeg.of()
    .withMode(TraverseMode.CAR)
    .withDistanceMeters(214.4)
    .withStartTime(TIME)
    .withEndTime(TIME.plusHours(1))
    .build();

  private static final Route ROUTE_WITH_EMISSIONS = TimetableRepositoryForTest.route(
    id("1")
  ).build();
  private static final Route ROUTE_WITH_ZERO_EMISSIONS = TimetableRepositoryForTest.route(
    id("2")
  ).build();
  private static final Route ROUTE_WITHOUT_EMISSIONS_CONFIGURED = TimetableRepositoryForTest.route(
    id("3")
  ).build();

  @BeforeAll
  static void SetUp() {
    Map<FeedScopedId, Double> emissions = new HashMap<>();
    emissions.put(new FeedScopedId("F", "1"), (0.12 / 12));
    emissions.put(new FeedScopedId("F", "2"), 0.0);
    EmissionsDataModel emissionsDataModel = new EmissionsDataModel(emissions, 0.131);
    eService = new DefaultEmissionsService(emissionsDataModel);
    decorateWithEmission = new DecorateWithEmission(eService);
  }

  @Test
  void testGetEmissionsForItinerary() {
    var i = createItinerary(createTransitLeg(ROUTE_WITH_EMISSIONS));
    i = decorateWithEmission.decorate(i);
    assertEquals(new Grams(2223.902), i.emissionsPerPerson().getCo2());
  }

  @Test
  void testGetEmissionsForCarRoute() {
    var i = createItinerary(STREET_LEG);
    i = decorateWithEmission.decorate(i);
    assertEquals(new Grams(28.0864), i.emissionsPerPerson().getCo2());
  }

  @Test
  void testNoEmissionsForFeedWithoutEmissionsConfigured() {
    var i = createItinerary(createTransitLeg(ROUTE_WITHOUT_EMISSIONS_CONFIGURED));
    i = decorateWithEmission.decorate(i);
    assertNull(i.emissionsPerPerson());
  }

  @Test
  void testZeroEmissionsForItineraryWithZeroEmissions() {
    var i = createItinerary(createTransitLeg(ROUTE_WITH_ZERO_EMISSIONS));
    i = decorateWithEmission.decorate(i);
    assertEquals(new Grams(0.0), i.emissionsPerPerson().getCo2());
  }

  @Test
  void testGetEmissionsForCombinedRoute() {
    var i = createItinerary(createTransitLeg(ROUTE_WITH_EMISSIONS), STREET_LEG);
    i = decorateWithEmission.decorate(i);
    assertEquals(new Grams(2251.9884), i.emissionsPerPerson().getCo2());
  }

  @Test
  void testNoEmissionsForCombinedRouteWithoutTransitEmissions() {
    var i = createItinerary(createTransitLeg(ROUTE_WITHOUT_EMISSIONS_CONFIGURED), STREET_LEG);
    i = decorateWithEmission.decorate(i);
    var emissionsResult = i.emissionsPerPerson() != null ? i.emissionsPerPerson().getCo2() : null;
    assertNull(emissionsResult);
  }

  private ScheduledTransitLeg createTransitLeg(Route route) {
    var stoptime = new StopTime();
    var stopTimes = new ArrayList<StopTime>();
    stopTimes.add(stoptime);
    var testModel = TimetableRepositoryForTest.of();
    var stopOne = testModel.stop("1:stop1", 60, 25).build();
    var stopTwo = testModel.stop("1:stop1", 61, 25).build();
    var stopThree = testModel.stop("1:stop1", 62, 25).build();
    var stopPattern = TimetableRepositoryForTest.stopPattern(stopOne, stopTwo, stopThree);
    var pattern = TimetableRepositoryForTest.tripPattern("1", route)
      .withStopPattern(stopPattern)
      .build();
    var trip = Trip.of(FeedScopedId.parse("FOO:BAR"))
      .withMode(TransitMode.BUS)
      .withRoute(route)
      .build();
    return new ScheduledTransitLegBuilder<>()
      .withTripTimes(TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator()))
      .withTripPattern(pattern)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(2)
      .withStartTime(TIME)
      .withEndTime(TIME.plusMinutes(10))
      .withServiceDate(TIME.toLocalDate())
      .withZoneId(ZoneIds.BERLIN)
      .withDistanceMeters(LegConstructionSupport.computeDistanceMeters(pattern, 0, 2))
      .build();
  }

  private static Itinerary createItinerary(Leg... legs) {
    return Itinerary.ofScheduledTransit(Arrays.asList(legs)).withGeneralizedCost(Cost.ZERO).build();
  }
}
