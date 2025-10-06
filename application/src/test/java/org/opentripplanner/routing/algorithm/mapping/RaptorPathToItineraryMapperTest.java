package org.opentripplanner.routing.algorithm.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptorlegacy._data.RaptorTestConstants.BOARD_SLACK;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexPathDurations;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.TransferPathLeg;
import org.opentripplanner.raptor.path.Path;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptorlegacy._data.api.TestPathBuilder;
import org.opentripplanner.raptorlegacy._data.transit.TestAccessEgress;
import org.opentripplanner.raptorlegacy._data.transit.TestRoute;
import org.opentripplanner.raptorlegacy._data.transit.TestTransitData;
import org.opentripplanner.raptorlegacy._data.transit.TestTripPattern;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultRaptorTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.FlexAccessEgressAdapter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfo;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.utils.time.TimeUtils;

public class RaptorPathToItineraryMapperTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final int BOARD_COST_SEC = 60;
  private static final int TRANSFER_COST_SEC = 120;
  private static final double[] TRANSIT_RELUCTANCE = new double[] { 1.0 };
  public static final double WAIT_RELUCTANCE = 0.8;
  private static final int[] STOP_COSTS = { 0, 0, 3_000, 0, 6_000, 0, 0, 0, 0, 0 };

  private static final int TRANSIT_START = TimeUtils.time("10:00");
  private static final int TRANSIT_END = TimeUtils.time("11:00");
  private static final Route ROUTE = TEST_MODEL.route("route").build();

  public static final RaptorCostCalculator<TestTripSchedule> COST_CALCULATOR =
    new DefaultCostCalculator<>(
      BOARD_COST_SEC,
      TRANSFER_COST_SEC,
      WAIT_RELUCTANCE,
      TRANSIT_RELUCTANCE,
      STOP_COSTS
    );

  private static final RegularStop S1 = TEST_MODEL.stop("STOP1").build();
  private static final RegularStop S2 = TEST_MODEL.stop("STOP2").build();
  private static final RegularStop S3 = TEST_MODEL.stop("STOP3").build();

  @ParameterizedTest
  @ValueSource(ints = { 0, 3000, -3000 })
  void createItineraryTestZeroDurationEgress(int lastLegCost) {
    // Arrange
    RaptorPathToItineraryMapper<TestTripSchedule> mapper = getRaptorPathToItineraryMapper();

    RaptorPath<TestTripSchedule> path = createTestTripSchedulePath(getTestTripSchedule()).egress(
      TestAccessEgress.free(2, RaptorCostConverter.toRaptorCost(lastLegCost))
    );

    int transitLegCost = path.accessLeg().nextLeg().c1();
    int egressLegCost = path.accessLeg().nextLeg().nextLeg().c1();

    // Act
    var itinerary = mapper.createItinerary(path);

    // Assert
    assertNotNull(itinerary);
    assertEquals(1, itinerary.legs().size(), "The wrong number of legs was returned");
    assertEquals(
      RaptorCostConverter.toOtpDomainCost(transitLegCost + egressLegCost),
      itinerary.legs().get(0).generalizedCost(),
      "Incorrect cost returned"
    );
  }

  @Test
  void noExtraLegWhenTransferringAtSameStop() {
    var mapper = getRaptorPathToItineraryMapper();

    var path = transferAtSameStopPath();
    var itinerary = mapper.createItinerary(path);
    assertThat(itinerary.legs().stream().map(Object::getClass)).doesNotContain(StreetLeg.class);
  }

  @Test
  void extraLegWhenTransferringAtSameStop() {
    RaptorPathToItineraryMapper<TestTripSchedule> mapper = getRaptorPathToItineraryMapper();

    var schedule = getTestTripSchedule2();
    var path = new TestPathBuilder(COST_CALCULATOR)
      .access(TRANSIT_START - BOARD_SLACK, 1)
      .bus(schedule, 2)
      .bus(schedule, 1)
      .egress(TestAccessEgress.free(1, RaptorCostConverter.toRaptorCost(100)));

    OTPFeature.ExtraTransferLegOnSameStop.testOn(() -> {
      var itinerary = mapper.createItinerary(path);
      assertEquals(
        List.of(ScheduledTransitLeg.class, StreetLeg.class, ScheduledTransitLeg.class),
        itinerary.legs().stream().map(Leg::getClass).toList()
      );
    });
  }

  @Test
  @Disabled("Need to write a general test framework to enable this.")
  void penalty() {
    // Arrange
    RaptorPathToItineraryMapper<TestTripSchedule> mapper = getRaptorPathToItineraryMapper();

    var penalty = new TimeAndCost(Duration.ofMinutes(10), Cost.costOfMinutes(10));
    // TODO - The TestAccessEgress is an internal Raptor test dummy class and is not allowed
    //        to be used outside raptor and optimized transfers. Also, the Itinerary mapper
    //        expect the generic type DefaultTripSchedule and not TestTripSchedule - it is pure
    //        luck that it works..
    // RaptorPath<TestTripSchedule> path = createTestTripSchedulePath(getTestTripSchedule())
    //   .egress(TestAccessEgress.car(2, RaptorCostConverter.toRaptorCost(1000), penalty));

    // Act
    var itinerary = mapper.createItinerary(null);

    // Assert
    assertNotNull(itinerary);
    assertEquals(4708, itinerary.generalizedCost());
    assertNotEquals(4708, itinerary.generalizedCostIncludingPenalty().toSeconds());
  }

  /**
   * Create a minimalist path FlexAccess-->Transfer-->Egress (without transit) and check that the 3 legs
   * are properly mapped in the itinerary.
   */
  @Test
  void createItineraryWithOnBoardFlexAccess() {
    RaptorPathToItineraryMapper<TestTripSchedule> mapper = getRaptorPathToItineraryMapper();

    var flexTrip = TEST_MODEL.unscheduledTrip(
      "flex",
      TEST_MODEL.stop("A:Stop:1").build(),
      TEST_MODEL.stop("A:Stop:2").build()
    );

    State state = TestStateBuilder.ofWalking().streetEdge().streetEdge().build();
    FlexAccessEgress flexAccessEgress = new FlexAccessEgress(
      S1,
      new FlexPathDurations(0, (int) state.getElapsedTimeSeconds(), 0, 0),
      0,
      1,
      flexTrip,
      state,
      true,
      RoutingBookingInfo.NOT_SET
    );
    RaptorAccessEgress access = new FlexAccessEgressAdapter(
      flexAccessEgress,
      AccessEgressType.ACCESS
    );
    Transfer transfer = new Transfer(S2.getIndex(), 0, EnumSet.of(StreetMode.WALK));
    RaptorTransfer raptorTransfer = new DefaultRaptorTransfer(S1.getIndex(), 0, 0, transfer);
    RaptorAccessEgress egress = new DefaultAccessEgress(S2.getIndex(), state);
    PathLeg<RaptorTripSchedule> egressLeg = new EgressPathLeg<>(egress, 0, 0, 0);
    PathLeg<RaptorTripSchedule> transferLeg = new TransferPathLeg<>(
      S1.getIndex(),
      0,
      0,
      0,
      raptorTransfer,
      egressLeg
    );
    AccessPathLeg<TestTripSchedule> accessLeg = new AccessPathLeg(access, 0, 0, 0, transferLeg);
    RaptorPath<TestTripSchedule> path = new Path<>(0, accessLeg, 0);
    // Act
    var itinerary = mapper.createItinerary(path);

    // Assert
    assertNotNull(itinerary);
    assertEquals(3, itinerary.legs().size(), "The wrong number of legs was returned");
  }

  private RaptorPath<TestTripSchedule> transferAtSameStopPath() {
    var schedule = transferAtSameStopSchedule();
    return new TestPathBuilder(COST_CALCULATOR)
      .access(TRANSIT_START, 1)
      .bus(schedule, 2)
      .bus(schedule, 1)
      .egress(TestAccessEgress.free(1, RaptorCostConverter.toRaptorCost(100)));
  }

  private TestTripSchedule transferAtSameStopSchedule() {
    TestTransitData data = new TestTransitData();
    var pattern = TestTripPattern.pattern("TestPattern", 1, 2, 3, 2, 1).withRoute(ROUTE);

    var timetable = new TestTripSchedule.Builder()
      .times(
        TimeUtils.time("10:00"),
        TimeUtils.time("10:05"),
        TimeUtils.time("10:10"),
        TimeUtils.time("10:15"),
        TimeUtils.time("10:20")
      )
      .pattern(pattern)
      .originalPattern(getOriginalPattern(pattern))
      .build();

    data.withRoutes(TestRoute.route("TransferAtSameStop", 1, 2, 3, 2, 1).withTimetable(timetable));

    return data.getRoute(0).getTripSchedule(0);
  }

  @Test
  void isSearchWindowAware() {
    var mapper = getRaptorPathToItineraryMapper();

    var path = createTestTripSchedulePath(getTestTripSchedule()).egress(
      TestAccessEgress.free(2, RaptorCostConverter.toRaptorCost(100))
    );
    var itinerary = mapper.createItinerary(path);
    assertTrue(itinerary.isSearchWindowAware());
  }

  private TripPattern getOriginalPattern(TestTripPattern pattern) {
    var siteRepositoryBuilder = TEST_MODEL.siteRepositoryBuilder();
    ArrayList<StopTime> stopTimes = new ArrayList<>();

    for (int i = 0; i < pattern.numberOfStopsInPattern(); i++) {
      var stop = siteRepositoryBuilder
        .regularStop(new FeedScopedId("TestFeed", i + ""))
        .withCoordinate(0.0, 0.0)
        .build();
      var stopTime = new StopTime();
      stopTime.setPickupType(PickDrop.SCHEDULED);
      stopTime.setDropOffType(PickDrop.SCHEDULED);
      stopTime.setStop(stop);
      stopTimes.add(stopTime);
    }

    var builder = TripPattern.of(new FeedScopedId("TestFeed", "TestId"))
      .withRoute(pattern.route())
      .withStopPattern(new StopPattern(stopTimes));
    return builder.build();
  }

  private TestPathBuilder createTestTripSchedulePath(TestTripSchedule testTripSchedule) {
    TestPathBuilder pathBuilder = new TestPathBuilder(COST_CALCULATOR);
    return pathBuilder.access(TRANSIT_START - BOARD_SLACK, 1).bus(testTripSchedule, 2);
  }

  private RaptorPathToItineraryMapper<TestTripSchedule> getRaptorPathToItineraryMapper() {
    Instant dateTime = LocalDateTime.of(2022, Month.OCTOBER, 10, 12, 0, 0)
      .atZone(ZoneIds.STOCKHOLM)
      .toInstant();
    TimetableRepository timetableRepository = new TimetableRepository();
    timetableRepository.initTimeZone(ZoneIds.CET);
    return new RaptorPathToItineraryMapper<>(
      new Graph(),
      new DefaultTransitService(timetableRepository),
      getRaptorTransitData(),
      dateTime.atZone(ZoneIds.CET),
      RouteRequest.defaultValue()
    );
  }

  private static RaptorTransitData getRaptorTransitData() {
    return new RaptorTransitData(
      new HashMap<>(),
      null,
      null,
      TEST_MODEL.siteRepositoryBuilder()
        .withRegularStop(S1)
        .withRegularStop(S2)
        .withRegularStop(S3)
        .build(),
      null,
      null,
      null,
      null
    );
  }

  private TestTripSchedule getTestTripSchedule2() {
    TestTransitData data = new TestTransitData();
    var pattern = TestTripPattern.pattern("TestPattern", 1, 2, 3, 2, 1).withRoute(ROUTE);

    var timetable = new TestTripSchedule.Builder()
      .times(
        TimeUtils.time("10:00"),
        TimeUtils.time("10:05"),
        TimeUtils.time("10:10"),
        TimeUtils.time("10:15"),
        TimeUtils.time("10:20")
      )
      .pattern(pattern)
      .originalPattern(getOriginalPattern(pattern))
      .build();

    data.withRoutes(TestRoute.route("TestRoute_1", 1, 2, 3, 2, 1).withTimetable(timetable));

    return data.getRoute(0).getTripSchedule(0);
  }

  private TestTripSchedule getTestTripSchedule() {
    TestTransitData data = new TestTransitData();
    var pattern = TestTripPattern.pattern("TestPattern", 1, 2).withRoute(ROUTE);

    var timetable = new TestTripSchedule.Builder()
      .times(TRANSIT_START, TRANSIT_END)
      .pattern(pattern)
      .originalPattern(getOriginalPattern(pattern))
      .build();

    data.withRoutes(TestRoute.route("TestRoute_1", 1, 2).withTimetable(timetable));

    return data.getRoute(0).getTripSchedule(0);
  }

  /**
   * Test filtering of midnight crossing service days for trips arriving after the arriveBy cutoff. Verifies that
   * trips arriving after the cutoff are filtered out, even when Raptor's time-shifting makes them appear to arrive
   * before the cutoff.
   */
  @Test
  void testNoTripsAfterArriveBy() {
    // Service date Sunday 2025-10-05 00:00 CEST (Saturday 22:00 UTC)
    // Trip departs Monday 00:35 UTC - 95700s from service date
    // Trip arrives Monday 01:39 UTC - 99540s from service date
    // ArriveBy search Monday 00:01 UTC
    // Raptor shows arrival at  Sunday 23:46 UTC due to time shifting
    // Expected: Trip should be filtered out because the actual arrival is 01:39 UTC
    var cutoffTime = Instant.parse("2025-10-06T00:01:00Z");
    var schedule = getScheduleArrivingAfterCutoff();

    RaptorPathToItineraryMapper<TestTripSchedule> mapper =
      ArriveByMapperSupport.createMidnightArriveByMapper(cutoffTime, getRaptorTransitData());

    // Create a path that Raptor would return (with the time-shifted  values)
    // Raptor's time shifted values appear to arrive at 6360s = 01:46 local time (which is before cutoff)
    RaptorPath<TestTripSchedule> path = new TestPathBuilder(COST_CALCULATOR)
      .access(5640, 1)
      .bus(schedule, 2)
      .egress(TestAccessEgress.free(2, 0));

    // Map to itinerary which should return null due to filtering
    var itinerary = mapper.createItinerary(path);

    // Assert that the itinerary is filtered out (null) because actual arrival time 99540s
    // from service date is 01:39 UTC which is after 00:01 UTC
    assertNull(itinerary, "Midnight crossing trip arriving after cutoff should be filtered out");
  }

  /**
   * Schedule for times after midnight
   *
   * @return
   */
  private TestTripSchedule getScheduleArrivingAfterCutoff() {
    TestTransitData data = new TestTransitData();
    var pattern = TestTripPattern.pattern("NightTrain", 1, 2).withRoute(ROUTE);
    var timetable = new TestTripSchedule.Builder()
      .times(
        // from Sunday 00:00 CEST 95700s = 26:35:00 = Monday 02:35 CEST (00:35 UTC)
        ArriveByMapperSupport.AFTER_MIDNIGHT_DEPART,
        // from Sunday 00:00 CEST 99540s = 27:39:00 = Monday 03:39 CEST (01:39 UTC)
        ArriveByMapperSupport.AFTER_CUTOFF_ARRIVE
      )
      .pattern(pattern)
      .originalPattern(getOriginalPattern(pattern))
      .build();
    data.withRoutes(TestRoute.route("NightRoute", 1, 2).withTimetable(timetable));
    var schedule = data.getRoute(0).getTripSchedule(0);

    return wrapWithTripTimes(
      schedule,
      ArriveByMapperSupport.SUNDAY,
      ArriveByMapperSupport.AFTER_MIDNIGHT_DEPART,
      ArriveByMapperSupport.AFTER_CUTOFF_ARRIVE
    );
  }

  private TestTripSchedule wrapWithTripTimes(
    TestTripSchedule schedule,
    LocalDate serviceDate,
    int... times
  ) {
    var trip = TEST_MODEL.trip("TestTrip").withRoute(ROUTE).build();
    var timeString = ArriveByMapperSupport.formatTimeString(times);
    var tripTimes = ScheduledTripTimes.of()
      .withTrip(trip)
      .withArrivalTimes(timeString)
      .withDepartureTimes(timeString)
      .build();
    return new TestTripScheduleWithTripTimes(schedule, tripTimes, serviceDate);
  }

  /**
   * Wrapper class that delegates to TestTripSchedule but provides real TripTimes
   */
  private static class TestTripScheduleWithTripTimes extends TestTripSchedule {

    private final TripTimes tripTimes;
    LocalDate serviceDate;

    TestTripScheduleWithTripTimes(
      TestTripSchedule delegate,
      TripTimes tripTimes,
      LocalDate serviceDate
    ) {
      super(
        (TestTripPattern) delegate.pattern(),
        extractArrivalTimes(delegate),
        extractDepartureTimes(delegate),
        delegate.transitReluctanceFactorIndex(),
        delegate.wheelchairBoarding(),
        delegate.getOriginalTripPattern()
      );
      this.tripTimes = tripTimes;
      this.serviceDate = serviceDate;
    }

    @Override
    public TripTimes getOriginalTripTimes() {
      return tripTimes;
    }

    @Override
    public LocalDate getServiceDate() {
      return serviceDate;
    }

    private static int[] extractArrivalTimes(TestTripSchedule delegate) {
      int stops = delegate.pattern().numberOfStopsInPattern();
      int[] times = new int[stops];
      for (int i = 0; i < stops; i++) {
        times[i] = delegate.arrival(i);
      }
      return times;
    }

    private static int[] extractDepartureTimes(TestTripSchedule delegate) {
      int stops = delegate.pattern().numberOfStopsInPattern();
      int[] times = new int[stops];
      for (int i = 0; i < stops; i++) {
        times[i] = delegate.departure(i);
      }
      return times;
    }
  }

  /**
   * Test that normal arriveBy searches (not crossing midnight) still work correctly. Trips arriving before the cutoff
   * should NOT be filtered.
   */
  @Test
  void testAllowsTripsBeforeArriveBy() {
    // Cutoff: Monday 14:00 UTC
    var cutoffTime = Instant.parse("2025-10-06T14:00:00Z");
    var schedule = getNormalDaytimeSchedule();
    RaptorPathToItineraryMapper<TestTripSchedule> mapper =
      ArriveByMapperSupport.createDaytimeArriveByMapper(cutoffTime, getRaptorTransitData());
    RaptorPath<TestTripSchedule> path = new TestPathBuilder(COST_CALCULATOR)
      .access(ArriveByMapperSupport.BEFORE_CUTOFF_DEPART - BOARD_SLACK, 1)
      .bus(schedule, 2)
      .egress(TestAccessEgress.free(2, 0));
    var itinerary = mapper.createItinerary(path);

    // Assert: Normal trip arriving before cutoff should NOT be filtered
    assertNotNull(itinerary, "Trip arriving before cutoff should not be filtered");
    assertEquals(
      Instant.parse("2025-10-06T13:30:00Z"),
      itinerary.endTime().toInstant(),
      "Arrival time should be 13:30 UTC"
    );
  }

  private TestTripSchedule getNormalDaytimeSchedule() {
    TestTransitData data = new TestTransitData();
    var pattern = TestTripPattern.pattern("DayTrain", 1, 2).withRoute(ROUTE);

    var timetable = new TestTripSchedule.Builder()
      .times(ArriveByMapperSupport.BEFORE_CUTOFF_DEPART, ArriveByMapperSupport.BEFORE_CUTOFF_ARRIVE)
      .pattern(pattern)
      .originalPattern(getOriginalPattern(pattern))
      .build();

    data.withRoutes(TestRoute.route("DayRoute", 1, 2).withTimetable(timetable));
    var schedule = data.getRoute(0).getTripSchedule(0);

    return wrapWithTripTimes(
      schedule,
      ArriveByMapperSupport.MONDAY,
      +ArriveByMapperSupport.BEFORE_CUTOFF_DEPART,
      +ArriveByMapperSupport.BEFORE_CUTOFF_ARRIVE
    );
  }

  /**
   * Test that departBy searches are not affected by the filtering. The filter should only apply to arriveBy
   * searches.
   */
  @Test
  void testDepartByIsNotAffectedByFiltering() {
    var departTime = Instant.parse("2025-10-06T00:01:00+02:00");
    var schedule = getScheduleArrivingAfterCutoff();
    RaptorPathToItineraryMapper<TestTripSchedule> mapper =
      ArriveByMapperSupport.createMidnightDepartByMapper(departTime, getRaptorTransitData());

    RaptorPath<TestTripSchedule> path = new TestPathBuilder(COST_CALCULATOR)
      .access(ArriveByMapperSupport.AFTER_MIDNIGHT_DEPART - BOARD_SLACK, 1)
      .bus(schedule, 2)
      .egress(TestAccessEgress.free(2, 0));

    var itinerary = mapper.createItinerary(path);

    // Assert: departBy searches should not filter trips
    assertNotNull(itinerary, "departBy searches should not filter trips");
  }

  /**
   * Test edge case: Trip arriving exactly at cutoff time. Should NOT be filtered (we only filter trips arriving AFTER
   * cutoff).
   */
  @Test
  void testAllowsTripsAtArriveBy() {
    var cutoffTime = Instant.parse("2025-10-06T14:00:00Z");
    var schedule = getExactCutoffSchedule();
    RaptorPathToItineraryMapper<TestTripSchedule> mapper =
      ArriveByMapperSupport.createDaytimeArriveByMapper(cutoffTime, getRaptorTransitData());

    RaptorPath<TestTripSchedule> path = new TestPathBuilder(COST_CALCULATOR)
      .access(ArriveByMapperSupport.BEFORE_CUTOFF_DEPART - BOARD_SLACK, 1)
      .bus(schedule, 2)
      .egress(TestAccessEgress.free(2, 0));

    var itinerary = mapper.createItinerary(path);

    // Assert: Trip arriving exactly at cutoff should be allowed
    assertNotNull(
      itinerary,
      "Trip arriving exactly at cutoff should not be filtered (only AFTER cutoff is filtered)"
    );
  }

  private TestTripSchedule getExactCutoffSchedule() {
    TestTransitData data = new TestTransitData();
    var pattern = TestTripPattern.pattern("ExactTrain", 1, 2).withRoute(ROUTE);

    var timetable = new TestTripSchedule.Builder()
      .times(ArriveByMapperSupport.BEFORE_CUTOFF_DEPART, ArriveByMapperSupport.AT_CUTOFF_ARRIVE)
      .pattern(pattern)
      .originalPattern(getOriginalPattern(pattern))
      .build();

    data.withRoutes(TestRoute.route("ExactRoute", 1, 2).withTimetable(timetable));
    var schedule = data.getRoute(0).getTripSchedule(0);

    return wrapWithTripTimes(
      schedule,
      ArriveByMapperSupport.MONDAY,
      ArriveByMapperSupport.BEFORE_CUTOFF_DEPART,
      ArriveByMapperSupport.AT_CUTOFF_ARRIVE
    );
  }
}
