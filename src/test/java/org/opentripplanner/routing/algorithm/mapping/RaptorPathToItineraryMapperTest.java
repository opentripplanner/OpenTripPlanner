package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor._data.api.TestPathBuilder;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestRoute;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
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

  private static final int BOARD_COST_SEC = 60;
  private static final int TRANSFER_COST_SEC = 120;
  private static final double[] TRANSIT_RELUCTANCE = new double[] { 1.0 };
  public static final double WAIT_RELUCTANCE = 0.8;
  private static final int[] STOP_COSTS = { 0, 0, 3_000, 0, 6_000, 0, 0, 0, 0, 0 };

  private static final int ACCESS_START = TimeUtils.time("10:00");
  private static final int TRANSIT_END = TimeUtils.time("11:00");

  private final TestTransitData data = new TestTransitData();

  public static final CostCalculator<TestTripSchedule> COST_CALCULATOR = new DefaultCostCalculator<>(
    BOARD_COST_SEC,
    TRANSFER_COST_SEC,
    WAIT_RELUCTANCE,
    TRANSIT_RELUCTANCE,
    STOP_COSTS
  );

  @ParameterizedTest
  @ValueSource(strings = { "0", "3000", "-3000" })
  public void createItineraryTestZeroDurationEgress(int LAST_LEG_COST) {
    // Arrange
    RaptorPathToItineraryMapper<TestTripSchedule> mapper = getRaptorPathToItineraryMapper();

    Path<TestTripSchedule> path = getTestTripSchedulePath(getTestTripSchedule())
      .egress(
        TestAccessEgress.zeroDurationAccess(2, RaptorCostConverter.toRaptorCost(LAST_LEG_COST))
      );

    // Act
    var itineraries = mapper.createItinerary(path);
    // Assert
    assertNotNull(itineraries);
    assertEquals(1, itineraries.getLegs().size(), "The wrong number of legs was returned");
    assertEquals(
      TRANSIT_END - ACCESS_START + BOARD_COST_SEC + LAST_LEG_COST,
      itineraries.getLegs().get(0).getGeneralizedCost(),
      "Incorrect cost returned"
    );
  }

  private TripPattern getOriginalPattern(TestTripPattern pattern) {
    ArrayList<StopTime> stopTimes = new ArrayList<StopTime>();

    for (int i = 0; i < pattern.numberOfStopsInPattern(); i++) {
      var stop = RegularStop
        .of(new FeedScopedId("TestFeed", i + ""))
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

  private TestPathBuilder getTestTripSchedulePath(TestTripSchedule testTripSchedule) {
    TestPathBuilder pathBuilder = new TestPathBuilder(0, COST_CALCULATOR);
    return pathBuilder
      .access(ACCESS_START, TestAccessEgress.zeroDurationAccess(1, 0))
      .bus(testTripSchedule, 2);
  }

  private RaptorPathToItineraryMapper<TestTripSchedule> getRaptorPathToItineraryMapper() {
    Instant dateTime = LocalDateTime
      .of(2022, Month.OCTOBER, 10, 12, 0, 0)
      .atZone(ZoneIds.STOCKHOLM)
      .toInstant();
    return new RaptorPathToItineraryMapper<TestTripSchedule>(
      new Graph(),
      new DefaultTransitService(new TransitModel()),
      null,
      dateTime.atZone(ZoneIds.CET),
      new RouteRequest()
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
      .times(ACCESS_START, TRANSIT_END)
      .pattern(pattern)
      .originalPattern(getOriginalPattern(pattern))
      .build();

    data.withRoutes(TestRoute.route("TestRoute_1", 1, 2).withTimetable(timetable));

    return data.getRoute(0).getTripSchedule(0);
  }
}
