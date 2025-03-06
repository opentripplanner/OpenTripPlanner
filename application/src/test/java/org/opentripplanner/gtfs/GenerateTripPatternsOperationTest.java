package org.opentripplanner.gtfs;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.trip;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.TripDegenerate;
import org.opentripplanner.graph_builder.issues.TripUndefinedService;
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepository;

class GenerateTripPatternsOperationTest {

  private static SiteRepository siteRepository;
  private static RegularStop stopA;
  private static RegularStop stopB;
  private static RegularStop stopC;
  private static Trip trip1;
  private static Trip trip2;
  private static Trip trip3;
  private static Trip trip4;
  private static Trip trip5;
  private static StopTime stopTimeA;
  private static StopTime stopTimeB;
  private static StopTime stopTimeC;

  private Deduplicator deduplicator;
  private DataImportIssueStore issueStore;
  private OtpTransitServiceBuilder transitServiceBuilder;
  private GeometryProcessor geometryProcessor;

  @BeforeAll
  static void setupClass() {
    TimetableRepositoryForTest timetableRepositoryForTest = TimetableRepositoryForTest.of();
    stopA = timetableRepositoryForTest.stop("stopA").build();
    stopB = timetableRepositoryForTest.stop("stopB").build();
    stopC = timetableRepositoryForTest.stop("stopC").build();
    siteRepository = timetableRepositoryForTest
      .siteRepositoryBuilder()
      .withRegularStop(stopA)
      .withRegularStop(stopB)
      .withRegularStop(stopC)
      .build();

    stopTimeA = new StopTime();
    stopTimeA.setStop(stopA);
    stopTimeB = new StopTime();
    stopTimeB.setStop(stopB);
    stopTimeC = new StopTime();
    stopTimeC.setStop(stopC);

    FeedScopedId serviceId1 = TimetableRepositoryForTest.id("SERVICE_ID_1");
    trip1 = trip("TRIP_ID_1")
      .withServiceId(serviceId1)
      .withMode(TransitMode.RAIL)
      .withNetexSubmode("SUBMODE_1")
      .withDirection(Direction.INBOUND)
      .build();

    // same route, mode, submode and direction as trip1
    FeedScopedId serviceId2 = TimetableRepositoryForTest.id("SERVICE_ID_2");
    trip2 = trip("TRIP_ID_2")
      .withServiceId(serviceId2)
      .withRoute(trip1.getRoute())
      .withMode(trip1.getMode())
      .withNetexSubmode(trip1.getNetexSubMode().name())
      .withDirection(trip1.getDirection())
      .build();

    // same route, direction as trip1, different mode
    FeedScopedId serviceId3 = TimetableRepositoryForTest.id("SERVICE_ID_3");
    trip3 = trip("TRIP_ID_3")
      .withServiceId(serviceId3)
      .withRoute(trip1.getRoute())
      .withMode(TransitMode.BUS)
      .withDirection(trip1.getDirection())
      .build();

    // same route, mode, direction  as trip1, different submode
    FeedScopedId serviceId4 = TimetableRepositoryForTest.id("SERVICE_ID_4");
    trip4 = trip("TRIP_ID_4")
      .withServiceId(serviceId4)
      .withRoute(trip1.getRoute())
      .withMode(trip1.getMode())
      .withNetexSubmode("SUMODE_2")
      .withDirection(trip1.getDirection())
      .build();

    // same route, mode  as trip1, different direction
    FeedScopedId serviceId5 = TimetableRepositoryForTest.id("SERVICE_ID_5");
    trip5 = trip("TRIP_ID_5")
      .withServiceId(serviceId5)
      .withRoute(trip1.getRoute())
      .withMode(trip1.getMode())
      .withNetexSubmode(trip1.getNetexSubMode().name())
      .withDirection(Direction.OUTBOUND)
      .build();
  }

  @BeforeEach
  void setup() {
    deduplicator = new Deduplicator();
    issueStore = new DefaultDataImportIssueStore();
    transitServiceBuilder = new OtpTransitServiceBuilder(siteRepository, issueStore);
    double maxStopToShapeSnapDistance = 100;
    geometryProcessor = new GeometryProcessor(
      transitServiceBuilder,
      maxStopToShapeSnapDistance,
      issueStore
    );
  }

  @Test
  void testGenerateTripPatternsNoTrip() {
    Set<FeedScopedId> calendarServiceIds = Set.of();
    GenerateTripPatternsOperation generateTripPatternsOperation = new GenerateTripPatternsOperation(
      transitServiceBuilder,
      issueStore,
      deduplicator,
      calendarServiceIds,
      geometryProcessor
    );
    generateTripPatternsOperation.run();

    Assertions.assertTrue(transitServiceBuilder.getTripPatterns().isEmpty());
    Assertions.assertTrue(issueStore.listIssues().isEmpty());
  }

  @Test
  void testGenerateTripPatternsTripWithUndefinedService() {
    transitServiceBuilder.getTripsById().computeIfAbsent(trip1.getId(), feedScopedId -> trip1);
    Set<FeedScopedId> calendarServiceIds = Set.of();

    GenerateTripPatternsOperation generateTripPatternsOperation = new GenerateTripPatternsOperation(
      transitServiceBuilder,
      issueStore,
      deduplicator,
      calendarServiceIds,
      geometryProcessor
    );
    generateTripPatternsOperation.run();

    Assertions.assertTrue(transitServiceBuilder.getTripPatterns().isEmpty());
    Assertions.assertFalse(issueStore.listIssues().isEmpty());
    Assertions.assertInstanceOf(TripUndefinedService.class, issueStore.listIssues().getFirst());
  }

  @Test
  void testGenerateTripPatternsDegeneratedTrip() {
    transitServiceBuilder.getTripsById().computeIfAbsent(trip1.getId(), feedScopedId -> trip1);
    Set<FeedScopedId> calendarServiceIds = Set.of(trip1.getServiceId());

    GenerateTripPatternsOperation generateTripPatternsOperation = new GenerateTripPatternsOperation(
      transitServiceBuilder,
      issueStore,
      deduplicator,
      calendarServiceIds,
      geometryProcessor
    );
    generateTripPatternsOperation.run();

    Assertions.assertTrue(transitServiceBuilder.getTripPatterns().isEmpty());
    Assertions.assertFalse(issueStore.listIssues().isEmpty());
    Assertions.assertInstanceOf(TripDegenerate.class, issueStore.listIssues().getFirst());
  }

  @Test
  void testGenerateTripPatterns() {
    transitServiceBuilder.getTripsById().computeIfAbsent(trip1.getId(), feedScopedId -> trip1);
    Collection<StopTime> stopTimes = List.of(stopTimeA, stopTimeB);
    transitServiceBuilder.getStopTimesSortedByTrip().put(trip1, stopTimes);
    Set<FeedScopedId> calendarServiceIds = Set.of(trip1.getServiceId());

    GenerateTripPatternsOperation generateTripPatternsOperation = new GenerateTripPatternsOperation(
      transitServiceBuilder,
      issueStore,
      deduplicator,
      calendarServiceIds,
      geometryProcessor
    );
    generateTripPatternsOperation.run();

    Assertions.assertEquals(1, transitServiceBuilder.getTripPatterns().size());
    Assertions.assertTrue(issueStore.listIssues().isEmpty());
  }

  @Test
  void testGenerateTripPatterns2TripsSameStops() {
    transitServiceBuilder.getTripsById().computeIfAbsent(trip1.getId(), feedScopedId -> trip1);
    transitServiceBuilder.getTripsById().computeIfAbsent(trip2.getId(), feedScopedId -> trip2);
    Collection<StopTime> stopTimes = List.of(stopTimeA, stopTimeB);

    transitServiceBuilder.getStopTimesSortedByTrip().put(trip1, stopTimes);
    transitServiceBuilder.getStopTimesSortedByTrip().put(trip2, stopTimes);

    Set<FeedScopedId> calendarServiceIds = Set.of(trip1.getServiceId(), trip2.getServiceId());

    GenerateTripPatternsOperation generateTripPatternsOperation = new GenerateTripPatternsOperation(
      transitServiceBuilder,
      issueStore,
      deduplicator,
      calendarServiceIds,
      geometryProcessor
    );
    generateTripPatternsOperation.run();

    Assertions.assertEquals(1, transitServiceBuilder.getTripPatterns().size());
    Assertions.assertEquals(
      2,
      transitServiceBuilder
        .getTripPatterns()
        .values()
        .stream()
        .findFirst()
        .orElseThrow()
        .getScheduledTimetable()
        .getTripTimes()
        .size()
    );
    Assertions.assertTrue(issueStore.listIssues().isEmpty());
  }

  @Test
  void testGenerateTripPatterns2TripsDifferentStops() {
    transitServiceBuilder.getTripsById().computeIfAbsent(trip1.getId(), feedScopedId -> trip1);
    transitServiceBuilder.getTripsById().computeIfAbsent(trip2.getId(), feedScopedId -> trip2);
    Collection<StopTime> stopTimesTrip1 = List.of(stopTimeA, stopTimeB);
    Collection<StopTime> stopTimesTrip2 = List.of(stopTimeA, stopTimeC);

    transitServiceBuilder.getStopTimesSortedByTrip().put(trip1, stopTimesTrip1);
    transitServiceBuilder.getStopTimesSortedByTrip().put(trip2, stopTimesTrip2);

    Set<FeedScopedId> calendarServiceIds = Set.of(trip1.getServiceId(), trip2.getServiceId());

    GenerateTripPatternsOperation generateTripPatternsOperation = new GenerateTripPatternsOperation(
      transitServiceBuilder,
      issueStore,
      deduplicator,
      calendarServiceIds,
      geometryProcessor
    );
    generateTripPatternsOperation.run();

    Assertions.assertEquals(2, transitServiceBuilder.getTripPatterns().size());
    Assertions.assertTrue(issueStore.listIssues().isEmpty());
  }

  static List<Arguments> testCases() {
    return List.of(
      // trips with different modes
      Arguments.of(trip1, trip3),
      // trips with different sub-modes
      Arguments.of(trip1, trip4),
      // trips with different directions
      Arguments.of(trip1, trip5)
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testGenerateDifferentTripPatterns(Trip t1, Trip t2) {
    transitServiceBuilder.getTripsById().computeIfAbsent(t1.getId(), feedScopedId -> t1);
    transitServiceBuilder.getTripsById().computeIfAbsent(t2.getId(), feedScopedId -> t2);
    Collection<StopTime> stopTimes = List.of(stopTimeA, stopTimeB);

    transitServiceBuilder.getStopTimesSortedByTrip().put(t1, stopTimes);
    transitServiceBuilder.getStopTimesSortedByTrip().put(t2, stopTimes);

    Set<FeedScopedId> calendarServiceIds = Set.of(t1.getServiceId(), t2.getServiceId());

    GenerateTripPatternsOperation generateTripPatternsOperation = new GenerateTripPatternsOperation(
      transitServiceBuilder,
      issueStore,
      deduplicator,
      calendarServiceIds,
      geometryProcessor
    );
    generateTripPatternsOperation.run();

    Assertions.assertEquals(2, transitServiceBuilder.getTripPatterns().size());
    Assertions.assertTrue(issueStore.listIssues().isEmpty());
  }
}
