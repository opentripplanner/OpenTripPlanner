package org.opentripplanner.ext.emission;

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
import org.opentripplanner.ext.emission.internal.DefaultEmissionRepository;
import org.opentripplanner.ext.emission.internal.DefaultEmissionService;
import org.opentripplanner.ext.emission.internal.itinerary.EmissionItineraryDecorator;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.plan.Emission;
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

/**
 * @deprecated The purpose of this test is not clear?
 * - Is it an integration test, it does notload data from file.
 * - Or, is it a unit-test on the {@link EmissionItineraryDecorator}? The test package/name does not
 *   reflect this and there is realy no need to create a
 *
 */
@Deprecated
class EmissionTest {

  private static DefaultEmissionService eService;
  private static EmissionItineraryDecorator emissionDecorator;

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
  private static final Route ROUTE_WITH_UNKNOWN_EMISSIONS = TimetableRepositoryForTest.route(
    id("2")
  ).build();
  private static final Route ROUTE_WITHOUT_EMISSIONS_CONFIGURED = TimetableRepositoryForTest.route(
    id("3")
  ).build();

  @BeforeAll
  static void SetUp() {
    Map<FeedScopedId, Emission> emission = new HashMap<>();
    emission.put(id("1"), Emission.co2_g(.12 / 12));
    emission.put(id("2"), Emission.co2_g(0.0));
    EmissionRepository emissionRepository = new DefaultEmissionRepository();
    emissionRepository.addRouteEmissions(emission);
    emissionRepository.setCarAvgCo2PerMeter(0.131);
    eService = new DefaultEmissionService(emissionRepository);
    emissionDecorator = new EmissionItineraryDecorator(eService);
  }

  @Test
  void testGetEmissionsForItinerary() {
    var itinerary = createItinerary(createTransitLeg(ROUTE_WITH_EMISSIONS));
    itinerary = emissionDecorator.decorate(itinerary);
    assertEquals(Emission.co2_g(2223.902), itinerary.emissionPerPerson());
  }

  @Test
  void testGetEmissionsForCarRoute() {
    var itinerary = createItinerary(STREET_LEG);
    itinerary = emissionDecorator.decorate(itinerary);
    assertEquals(Emission.co2_g(28.0864), itinerary.emissionPerPerson());
  }

  @Test
  void testNoEmissionsForFeedWithoutEmissionsConfigured() {
    var itinerary = createItinerary(createTransitLeg(ROUTE_WITHOUT_EMISSIONS_CONFIGURED));
    itinerary = emissionDecorator.decorate(itinerary);
    assertNull(itinerary.emissionPerPerson());
  }

  @Test
  void testZeroEmissionsForItineraryWithZeroEmissions() {
    var itinerary = createItinerary(createTransitLeg(ROUTE_WITH_UNKNOWN_EMISSIONS));
    itinerary = emissionDecorator.decorate(itinerary);
    assertNull(itinerary.emissionPerPerson());
  }

  @Test
  void testGetEmissionsForCombinedRoute() {
    var itinerary = createItinerary(createTransitLeg(ROUTE_WITH_EMISSIONS), STREET_LEG);
    itinerary = emissionDecorator.decorate(itinerary);
    assertEquals(Emission.co2_g(2251.9884), itinerary.emissionPerPerson());
  }

  @Test
  void testNoEmissionsForCombinedRouteWithoutTransitEmissions() {
    var itinerary = createItinerary(
      createTransitLeg(ROUTE_WITHOUT_EMISSIONS_CONFIGURED),
      STREET_LEG
    );
    itinerary = emissionDecorator.decorate(itinerary);
    assertNull(itinerary.emissionPerPerson());
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
    var trip = Trip.of(id("FOO:BAR")).withMode(TransitMode.BUS).withRoute(route).build();
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
