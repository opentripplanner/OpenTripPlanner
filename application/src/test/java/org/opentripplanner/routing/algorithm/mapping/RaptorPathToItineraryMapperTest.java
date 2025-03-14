package org.opentripplanner.routing.algorithm.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptorlegacy._data.RaptorTestConstants.BOARD_SLACK;

import java.time.Duration;
import java.time.Instant;
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
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;
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
  private static final Route ROUTE = TimetableRepositoryForTest.route("route").build();

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
      itinerary.legs().get(0).getGeneralizedCost(),
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
      new RouteRequest()
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
}
