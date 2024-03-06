package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.raptor._data.RaptorTestConstants.BOARD_SLACK;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor._data.api.TestPathBuilder;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestRoute;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.TransferPathLeg;
import org.opentripplanner.raptor.path.Path;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultRaptorTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.FlexAccessEgressAdapter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

public class RaptorPathToItineraryMapperTest {

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();
  private static final int BOARD_COST_SEC = 60;
  private static final int TRANSFER_COST_SEC = 120;
  private static final double[] TRANSIT_RELUCTANCE = new double[] { 1.0 };
  public static final double WAIT_RELUCTANCE = 0.8;
  private static final int[] STOP_COSTS = { 0, 0, 3_000, 0, 6_000, 0, 0, 0, 0, 0 };

  private static final int TRANSIT_START = TimeUtils.time("10:00");
  private static final int TRANSIT_END = TimeUtils.time("11:00");

  private final TestTransitData data = new TestTransitData();

  public static final RaptorCostCalculator<TestTripSchedule> COST_CALCULATOR = new DefaultCostCalculator<>(
    BOARD_COST_SEC,
    TRANSFER_COST_SEC,
    WAIT_RELUCTANCE,
    TRANSIT_RELUCTANCE,
    STOP_COSTS
  );

  private static final RegularStop S1 = TEST_MODEL.stop("STOP1", 0.0, 0.0).build();

  private static final RegularStop S2 = TEST_MODEL.stop("STOP2", 1.0, 1.0).build();

  @ParameterizedTest
  @ValueSource(ints = { 0, 3000, -3000 })
  void createItineraryTestZeroDurationEgress(int lastLegCost) {
    // Arrange
    RaptorPathToItineraryMapper<TestTripSchedule> mapper = getRaptorPathToItineraryMapper();

    RaptorPath<TestTripSchedule> path = createTestTripSchedulePath(getTestTripSchedule())
      .egress(TestAccessEgress.free(2, RaptorCostConverter.toRaptorCost(lastLegCost)));

    int transitLegCost = path.accessLeg().nextLeg().c1();
    int egressLegCost = path.accessLeg().nextLeg().nextLeg().c1();

    // Act
    var itinerary = mapper.createItinerary(path);

    // Assert
    assertNotNull(itinerary);
    assertEquals(1, itinerary.getLegs().size(), "The wrong number of legs was returned");
    assertEquals(
      RaptorCostConverter.toOtpDomainCost(transitLegCost + egressLegCost),
      itinerary.getLegs().get(0).getGeneralizedCost(),
      "Incorrect cost returned"
    );
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
    assertEquals(4708, itinerary.getGeneralizedCost());
    assertNotEquals(4708, itinerary.getGeneralizedCostIncludingPenalty());
  }

  /**
   * Create a minimalist path FlexAccess-->Transfer-->Egress (without transit) and check that the 3 legs
   * are properly mapped in the itinerary.
   */
  @Test
  void createItineraryWithOnBoardFlexAccess() {
    RaptorPathToItineraryMapper<TestTripSchedule> mapper = getRaptorPathToItineraryMapper();

    State state = TestStateBuilder.ofWalking().streetEdge().streetEdge().build();
    FlexAccessEgress flexAccessEgress = new FlexAccessEgress(S1, null, 0, 1, null, state, true);
    RaptorAccessEgress access = new FlexAccessEgressAdapter(flexAccessEgress, false);
    Transfer transfer = new Transfer(S2.getIndex(), 0);
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
    assertEquals(3, itinerary.getLegs().size(), "The wrong number of legs was returned");
  }

  private TripPattern getOriginalPattern(TestTripPattern pattern) {
    var stopModelBuilder = TEST_MODEL.stopModelBuilder();
    ArrayList<StopTime> stopTimes = new ArrayList<>();

    for (int i = 0; i < pattern.numberOfStopsInPattern(); i++) {
      var stop = stopModelBuilder
        .regularStop(new FeedScopedId("TestFeed", i + ""))
        .withCoordinate(0.0, 0.0)
        .build();
      var stopTime = new StopTime();
      stopTime.setPickupType(PickDrop.SCHEDULED);
      stopTime.setDropOffType(PickDrop.SCHEDULED);
      stopTime.setStop(stop);
      stopTimes.add(stopTime);
    }

    var builder = TripPattern
      .of(new FeedScopedId("TestFeed", "TestId"))
      .withRoute(pattern.route())
      .withStopPattern(new StopPattern(stopTimes));
    return builder.build();
  }

  private TestPathBuilder createTestTripSchedulePath(TestTripSchedule testTripSchedule) {
    TestPathBuilder pathBuilder = new TestPathBuilder(COST_CALCULATOR);
    return pathBuilder.access(TRANSIT_START - BOARD_SLACK, 1).bus(testTripSchedule, 2);
  }

  private RaptorPathToItineraryMapper<TestTripSchedule> getRaptorPathToItineraryMapper() {
    Instant dateTime = LocalDateTime
      .of(2022, Month.OCTOBER, 10, 12, 0, 0)
      .atZone(ZoneIds.STOCKHOLM)
      .toInstant();
    TransitModel transitModel = new TransitModel();
    transitModel.initTimeZone(ZoneIds.CET);
    return new RaptorPathToItineraryMapper<>(
      new Graph(),
      new DefaultTransitService(transitModel),
      getTransitLayer(),
      dateTime.atZone(ZoneIds.CET),
      new RouteRequest()
    );
  }

  private static TransitLayer getTransitLayer() {
    return new TransitLayer(
      new HashMap<>(),
      null,
      null,
      TEST_MODEL.stopModelBuilder().withRegularStop(S1).withRegularStop(S2).build(),
      null,
      null,
      null,
      null,
      null
    );
  }

  private TestTripSchedule getTestTripSchedule() {
    var agency = Agency
      .of(new FeedScopedId("TestFeed", "Auth_1"))
      .withName("Test_Agency")
      .withTimezone("Europe/Stockholm")
      .build();

    var route = Route
      .of(new FeedScopedId("TestFeed", "Line_1"))
      .withAgency(agency)
      .withMode(TransitMode.BUS)
      .withShortName("Test_Bus")
      .build();

    var pattern = TestTripPattern.pattern("TestPattern", 1, 2).withRoute(route);

    var timetable = new TestTripSchedule.Builder()
      .times(TRANSIT_START, TRANSIT_END)
      .pattern(pattern)
      .originalPattern(getOriginalPattern(pattern))
      .build();

    data.withRoutes(TestRoute.route("TestRoute_1", 1, 2).withTimetable(timetable));

    return data.getRoute(0).getTripSchedule(0);
  }
}
