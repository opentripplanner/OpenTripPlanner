package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptorlegacy._data.transit.TestRoute.route;
import static org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule.schedule;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.api.TestPathBuilder;
import org.opentripplanner.raptorlegacy._data.transit.TestRoute;
import org.opentripplanner.raptorlegacy._data.transit.TestTransfers;
import org.opentripplanner.raptorlegacy._data.transit.TestTransitData;
import org.opentripplanner.raptorlegacy._data.transit.TestTripPattern;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.utils.time.TimeUtils;

public class TransferGeneratorTest implements RaptorTestConstants {

  // Given a total slack of 30 seconds
  private static final int BOARD_SLACK = 10;
  private static final int TRANSFER_SLACK = 15;
  private static final int ALIGHT_SLACK = 5;
  // Access walk start 1 minute before departure
  private static final int ACCESS_START = TimeUtils.time("10:00");
  private static final int ACCESS_DURATION = D1m;

  private static final RaptorSlackProvider SLACK_PROVIDER = new DefaultSlackProvider(
    TRANSFER_SLACK,
    BOARD_SLACK,
    ALIGHT_SLACK
  );

  private final TestPathBuilder pathBuilder = new TestPathBuilder(SLACK_PROVIDER, COST_CALCULATOR);

  private final TestTransitData data = new TestTransitData().withSlackProvider(SLACK_PROVIDER);

  private final TransferServiceAdaptor<TestTripSchedule> tsAdaptor = data.transferServiceAdaptor();

  @Test
  void findTransferPathWithoutTransfers() {
    data.withRoutes(
      route(TestTripPattern.pattern("L1", STOP_A, STOP_B, STOP_C).withSlackIndex(1))
        .withTimetable(schedule("10:00 10:20 10:30"))
    );
    var schedule = data.getRoute(0).getTripSchedule(0);

    var path = pathBuilder
      .access(ACCESS_START, STOP_A, ACCESS_DURATION)
      .bus(schedule, STOP_C)
      .egress(D1m);

    var transitLegs = path.transitLegs().collect(Collectors.toList());

    var subject = new TransferGenerator<>(tsAdaptor, data);

    assertEquals("[]", subject.findAllPossibleTransfers(transitLegs).toString());
  }

  @Test
  void findTransferForTheSameRoute() {
    data.withRoutes(
      route("L1", STOP_A, STOP_B, STOP_C, STOP_D)
        .withTimetable(schedule("10:02 10:10 10:20 10:30"), schedule("10:04 10:12 10:22 10:32"))
    );

    // The only possible place to transfer between A and C is stop B:
    var transitLegs = transitLegsSameRoute(STOP_A, STOP_B, STOP_C);
    var subject = new TransferGenerator<>(tsAdaptor, data);
    assertEquals(
      "[[TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:12 BUS L1]}]]",
      subject.findAllPossibleTransfers(transitLegs).toString()
    );

    // The only possible place to transfer between B and D is stop C:
    transitLegs = transitLegsSameRoute(STOP_B, STOP_C, STOP_D);
    subject = new TransferGenerator<>(tsAdaptor, data);
    assertEquals(
      "[[TripToTripTransfer{from: [3 10:20 BUS L1], to: [3 10:22 BUS L1]}]]",
      subject.findAllPossibleTransfers(transitLegs).toString()
    );

    // Between A and D transfers may happen at stop B and C. The transfers should be sorted on
    // the to-trip-departure-time (descending)
    transitLegs = transitLegsSameRoute(STOP_A, STOP_C, STOP_D);
    subject = new TransferGenerator<>(tsAdaptor, data);
    assertEquals(
      "[[TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:12 BUS L1]}, " +
      "TripToTripTransfer{from: [3 10:20 BUS L1], to: [3 10:22 BUS L1]}]]",
      subject.findAllPossibleTransfers(transitLegs).toString()
    );
  }

  @Test
  void findGuaranteedTransferWithNoSlack() {
    data.withRoutes(
      route("L1", STOP_A, STOP_B).withTimetable(schedule("10:10 10:20")),
      route("L2", STOP_B, STOP_C).withTimetable(schedule("10:20 10:30")),
      route("L3", STOP_D, STOP_E).withTimetable(schedule("10:31 10:40"))
    );

    var schedule1 = data.getRoute(0).getTripSchedule(0);
    var schedule2 = data.getRoute(1).getTripSchedule(0);
    var schedule3 = data.getRoute(2).getTripSchedule(0);

    data
      .withTransfer(STOP_C, TestTransfers.transfer(STOP_D, D1m))
      .withGuaranteedTransfer(schedule1, STOP_B, schedule2, STOP_B)
      .withGuaranteedTransfer(schedule2, STOP_C, schedule3, STOP_D);

    var path = pathBuilder
      .access(ACCESS_START, STOP_A, ACCESS_DURATION)
      .bus(schedule1, STOP_B)
      .bus(schedule2, STOP_C)
      .walk(D1m, STOP_D)
      .bus(schedule3, STOP_E)
      .egress(D1m);

    var transitLegs = path.transitLegs().collect(Collectors.toList());

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    assertEquals(
      "[[" +
      "TripToTripTransfer{from: [2 10:20 BUS L1], to: [2 10:20 BUS L2]}" +
      "], [" +
      "TripToTripTransfer{from: [3 10:30 BUS L2], to: [4 10:31 BUS L3], transfer: WALK 1m $120 ~ 4}" +
      "]]",
      result.toString()
    );
  }

  @Test
  void findTransferForDifferentRoutes() {
    TestRoute l1 = route("L1", STOP_A, STOP_B, STOP_C, STOP_D)
      .withTimetable(schedule("10:02 10:10 10:20 10:30"));
    TestRoute l2 = route("L2", STOP_E, STOP_C, STOP_F, STOP_G)
      .withTimetable(schedule("10:12 10:22 10:32 10:40"));

    // S
    data
      .withRoutes(l1, l2)
      .withTransfer(STOP_B, TestTransfers.transfer(STOP_E, D1m))
      .withTransfer(STOP_D, TestTransfers.transfer(STOP_F, D20s));

    // The only possible place to transfer between A and D is stop C (no extra transfers):
    var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_C, STOP_G);

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);
    assertEquals(
      "[[" +
      "TripToTripTransfer{from: [2 10:10 BUS L1], to: [5 10:12 BUS L2], transfer: WALK 1m $120 ~ 5}, " +
      "TripToTripTransfer{from: [3 10:20 BUS L1], to: [3 10:22 BUS L2]}, " +
      "TripToTripTransfer{from: [4 10:30 BUS L1], to: [6 10:32 BUS L2], transfer: WALK 20s $40 ~ 6}" +
      "]]",
      result.toString()
    );
  }

  @Test
  @DisplayName("Two transfers on same station with circular line")
  void findTransfersForCircularLine1() {
    var l1 = route("L1", STOP_A, STOP_C, STOP_D).withTimetable(schedule("10:02 10:10 10:20"));
    // Passing same stop two times
    var l2 = route("L2", STOP_D, STOP_E, STOP_D, STOP_F)
      .withTimetable(schedule("10:30 10:40 10:50 11:00"));

    data.withRoutes(l1, l2);

    var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_D, STOP_F);

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    // Ensure that there are two possible transfers generated
    assertEquals(
      "[[TripToTripTransfer{from: [4 10:20 BUS L1], to: [4 10:30 BUS L2]}, TripToTripTransfer{from: [4 10:20 BUS L1], to: [4 10:50 BUS L2]}]]",
      result.toString()
    );
  }

  @Test
  @DisplayName("Transfer with walk between stations with circular line")
  void findTransfersForCircularLine2() {
    var l1 = route("L1", STOP_A, STOP_C, STOP_D).withTimetable(schedule("10:02 10:10 10:20"));
    // Passing same stop two times
    var l2 = route("L2", STOP_E, STOP_F, STOP_E, STOP_G)
      .withTimetable(schedule("10:30 10:40 10:50 11:00"));

    data.withRoutes(l1, l2).withTransfer(STOP_D, TestTransfers.transfer(STOP_E, D1m));

    var schedule1 = data.getRoute(0).getTripSchedule(0);
    var schedule2 = data.getRoute(1).getTripSchedule(0);

    var path = pathBuilder
      .access(ACCESS_START, STOP_A, ACCESS_DURATION)
      .bus(schedule1, STOP_D)
      .walk(D1m, STOP_E)
      .bus(schedule2, STOP_G)
      .egress(D1m);

    var transitLegs = path.transitLegs().collect(Collectors.toList());

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    assertEquals(
      "[[" +
      "TripToTripTransfer{from: [4 10:20 BUS L1], to: [5 10:30 BUS L2], transfer: WALK 1m $120 ~ 5}, " +
      "TripToTripTransfer{from: [4 10:20 BUS L1], to: [5 10:50 BUS L2], transfer: WALK 1m $120 ~ 5}" +
      "]]",
      result.toString()
    );
  }

  @Test
  @DisplayName("Single transfer on same station with circular line")
  void findTransfersForCircularLine3() {
    var l1 = route("L1", STOP_A, STOP_C, STOP_D).withTimetable(schedule("10:02 10:10 10:20"));
    // Passing same stop two times
    var l2 = route("L2", STOP_D, STOP_E, STOP_D, STOP_F)
      .withTimetable(schedule("10:10 10:20 10:30 10:40"));

    data.withRoutes(l1, l2);

    var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_D, STOP_F);

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    // Ensure that there is only one transfer
    assertEquals(
      "[[TripToTripTransfer{from: [4 10:20 BUS L1], to: [4 10:30 BUS L2]}]]",
      result.toString()
    );
  }

  @Test
  @DisplayName("Transfer on same station with circular line with boarding forbidden")
  void findTransfersForCircularLine4() {
    var l1 = route("L1", STOP_A, STOP_C, STOP_D).withTimetable(schedule("10:02 10:10 10:20"));

    var p2 = TestTripPattern.pattern("L2", STOP_D, STOP_E, STOP_D, STOP_F);
    // Only allow alighting on third stop
    p2.restrictions("* * A *");
    var l2 = route(p2).withTimetable(schedule("10:30 10:40 10:50 11:00"));

    data.withRoutes(l1, l2);

    var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_D, STOP_F);

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    // Only one transfer in result
    // Since the second one was restricted to only alighting
    assertEquals(
      "[[TripToTripTransfer{from: [4 10:20 BUS L1], to: [4 10:30 BUS L2]}]]",
      result.toString()
    );
  }

  @Test
  @DisplayName("Transfer on same station with circular line with transfer constraint")
  void findTransfersForCircularLine5() {
    var l1 = route("L1", STOP_A, STOP_C, STOP_D).withTimetable(schedule("10:02 10:10 10:20"));
    // Passing same stop two times
    var l2 = route("L2", STOP_E, STOP_F, STOP_E, STOP_G)
      .withTimetable(schedule("10:30 10:40 10:50 11:00"));

    data.withRoutes(l1, l2).withTransfer(STOP_D, TestTransfers.transfer(STOP_E, D1m));

    var schedule1 = data.getRoute(0).getTripSchedule(0);
    var schedule2 = data.getRoute(1).getTripSchedule(0);

    var tripA = l1.getTripSchedule(0);
    var tripB = l2.getTripSchedule(0);

    data.withConstrainedTransfer(tripA, STOP_D, tripB, STOP_E, TestTransitData.TX_NOT_ALLOWED);

    var path = pathBuilder
      .access(ACCESS_START, STOP_A, ACCESS_DURATION)
      .bus(schedule1, STOP_D)
      .walk(D1m, STOP_E)
      .bus(schedule2, STOP_G)
      .egress(D1m);

    var transitLegs = path.transitLegs().collect(Collectors.toList());
    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    // Only transfer on second visit
    // since first one is constrained
    assertEquals(
      "[[TripToTripTransfer{from: [4 10:20 BUS L1], to: [5 10:50 BUS L2], transfer: WALK 1m $120 ~ 5}]]",
      result.toString()
    );
  }

  @Test
  @DisplayName("Transfer on same station with circular line calculates correctly on stop order")
  void findTransfersForCircularLine6() {
    var l1 = route("L1", STOP_A, STOP_C, STOP_D).withTimetable(schedule("10:02 10:10 10:20"));
    // Passing same stop two times
    var l2 = route("L2", STOP_D, STOP_E, STOP_F, STOP_D)
      .withTimetable(schedule("10:30 10:40 10:50 11:00"));

    data.withRoutes(l1, l2);

    var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_D, STOP_F);

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    // Only one possible transfer generated since second visit happens after stop F
    assertEquals(
      "[[TripToTripTransfer{from: [4 10:20 BUS L1], to: [4 10:30 BUS L2]}]]",
      result.toString()
    );
  }

  @Test
  void findTransferForDifferentRoutesWithCustomBoardingSlack() {
    RaptorSlackProvider slackProvider = new DefaultSlackProvider(0, 0, 0) {
      @Override
      public int boardSlack(int slackIndex) {
        return slackIndex == 1 ? 20 * 60 : 0;
      }
    };
    data.withSlackProvider(slackProvider);

    TestRoute l1 = route("L1", STOP_A, STOP_B).withTimetable(schedule("10:00 10:10"));
    TestRoute l2 = route("L2", STOP_B, STOP_C).withTimetable(schedule("10:20 10:30"));

    // S
    data.withRoutes(l1, l2);

    // The only possible place to transfer between A and D is stop C (no extra transfers):
    var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_B, STOP_C);

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);
    assertEquals(
      "[[" + "TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:20 BUS L2]}" + "]]",
      result.toString()
    );
  }

  @Test
  void findTransferWithAlightingForbiddenAtSameStop() {
    TestTripPattern p1 = TestTripPattern.pattern("L1", STOP_A, STOP_B, STOP_C);
    p1.restrictions("B B A");

    TestRoute l1 = route(p1).withTimetable(schedule("10:00 10:10 10:20"));
    TestRoute l2 = route("L2", STOP_B, STOP_D, STOP_E).withTimetable(schedule("10:20 10:30 10:40"));

    data.withRoutes(l1, l2).withTransfer(STOP_C, TestTransfers.transfer(STOP_D, D1m));

    final RaptorPath<TestTripSchedule> path = pathBuilder
      .access(ACCESS_START, STOP_A, ACCESS_DURATION)
      .bus(l1.getTripSchedule(0), STOP_C)
      .walk(D1m, STOP_D)
      .bus(l2.getTripSchedule(0), STOP_E)
      .egress(D1m);

    var transitLegs = path.transitLegs().collect(Collectors.toList());

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    // Transfer at B is not allowed
    assertEquals(
      "[[" +
      "TripToTripTransfer{from: [3 10:20 BUS L1], to: [4 10:30 BUS L2], transfer: WALK 1m $120 ~ 4}" +
      "]]",
      result.toString()
    );
  }

  @Test
  void findTransferWithBoardingForbiddenAtSameStop() {
    TestRoute l1 = route("L1", STOP_A, STOP_B, STOP_C).withTimetable(schedule("10:00 10:10 10:20"));

    TestTripPattern p2 = TestTripPattern.pattern("L2", STOP_B, STOP_D, STOP_E);
    p2.restrictions("A BA A");

    TestRoute l2 = route(p2).withTimetable(schedule("10:20 10:30 10:40"));

    data.withRoutes(l1, l2).withTransfer(STOP_C, TestTransfers.transfer(STOP_D, D1m));

    final RaptorPath<TestTripSchedule> path = pathBuilder
      .access(ACCESS_START, STOP_A, ACCESS_DURATION)
      .bus(l1.getTripSchedule(0), STOP_C)
      .walk(D1m, STOP_D)
      .bus(l2.getTripSchedule(0), STOP_E)
      .egress(D1m);

    var transitLegs = path.transitLegs().collect(Collectors.toList());

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    // Transfer at B is not allowed
    assertEquals(
      "[[" +
      "TripToTripTransfer{from: [3 10:20 BUS L1], to: [4 10:30 BUS L2], transfer: WALK 1m $120 ~ 4}" +
      "]]",
      result.toString()
    );
  }

  @Test
  void findTransferWithAlightingForbiddenAtDifferentStop() {
    TestTripPattern p1 = TestTripPattern.pattern("L1", STOP_A, STOP_B, STOP_C);
    p1.restrictions("B A B");

    TestRoute l1 = route(p1).withTimetable(schedule("10:00 10:10 10:20"));
    TestRoute l2 = route("L2", STOP_B, STOP_D, STOP_E).withTimetable(schedule("10:20 10:30 10:40"));

    data.withRoutes(l1, l2).withTransfer(STOP_C, TestTransfers.transfer(STOP_D, D1m));

    var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_B, STOP_E);

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    // Transfer at C is not allowed
    assertEquals(
      "[[" + "TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:20 BUS L2]}" + "]]",
      result.toString()
    );
  }

  @Test
  void findTransferWithBoardingForbiddenAtDifferentStop() {
    TestRoute l1 = route("L1", STOP_A, STOP_B, STOP_C).withTimetable(schedule("10:00 10:10 10:20"));

    TestTripPattern p2 = TestTripPattern.pattern("L2", STOP_B, STOP_D, STOP_E);
    p2.restrictions("B A A");

    TestRoute l2 = route(p2).withTimetable(schedule("10:20 10:30 10:40"));

    data.withRoutes(l1, l2).withTransfer(STOP_C, TestTransfers.transfer(STOP_D, D1m));

    var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_B, STOP_E);

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    // Transfer at D is not allowed
    assertEquals(
      "[[" + "TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:20 BUS L2]}" + "]]",
      result.toString()
    );
  }

  @Test
  void findTransferWithNotAllowedConstrainedTransfer() {
    testThatThereIsNoTransferAtStopB(TestTransitData.TX_NOT_ALLOWED);
  }

  @Test
  void findTransferWithLongMinTimeTransfer() {
    // a very long minimum time is effectively the same thing as a not-allowed transfer
    testThatThereIsNoTransferAtStopB(TestTransitData.TX_LONG_MIN_TIME);
  }

  // TODO: here we check that minimum transfer time and slack are NOT added up, but perhaps that is
  // asserting the wrong behaviour
  static Stream<Arguments> minTransferTimeSlackCases() {
    return Stream.of(
      // transfer takes 1 min plus 0 slack, passenger will make it
      Arguments.of(ofMinutes(1), ofMinutes(0), true),
      // slack is 30 minutes, passenger won't make the connection
      Arguments.of(ofMinutes(1), ofMinutes(30), false),
      // tight since 8 minutes slack + 1 min transfer time but still less than the 10 minutes required
      Arguments.of(ofMinutes(1), ofMinutes(8), true),
      // transfer slack is ignored since minimumTransferTime is short
      Arguments.of(ofMinutes(1), ofMinutes(9), true),
      Arguments.of(ofMinutes(11), ofMinutes(0), false),
      Arguments.of(ofMinutes(9), ofMinutes(1), true),
      Arguments.of(ofMinutes(0), ofMinutes(11), false)
    );
  }

  @ParameterizedTest(
    name = "minimum transfer time of {0}, transfer slack of {1} should expectTransfer={2} on 10 min transfer window"
  )
  @MethodSource("minTransferTimeSlackCases")
  void includeTransferSlackInMinimumTransferTime(
    Duration minTransferTime,
    Duration transferSlack,
    boolean expectTransfer
  ) {
    TestRoute l1 = route("L1", STOP_A, STOP_B).withTimetable(schedule("10:00 10:10"));
    TestRoute l2 = route("L2", STOP_B, STOP_C).withTimetable(schedule("10:20 10:30"));

    // S
    data.withRoutes(l1, l2);

    var tripA = l1.getTripSchedule(0);
    var tripB = l2.getTripSchedule(0);
    var constraint = TransferConstraint
      .of()
      .minTransferTime((int) minTransferTime.getSeconds())
      .build();
    data.withConstrainedTransfer(tripA, STOP_B, tripB, STOP_B, constraint);

    // The only possible place to transfer between A and C is stop B (no extra transfers):
    var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_B, STOP_C);

    data.withSlackProvider(new DefaultSlackProvider((int) transferSlack.toSeconds(), 0, 0));

    var subject = new TransferGenerator<>(tsAdaptor, data);

    if (expectTransfer) {
      var result = subject.findAllPossibleTransfers(transitLegs);
      assertEquals(
        "[[TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:20 BUS L2]}]]",
        result.toString()
      );
    } else {
      Assertions.assertThrows(
        RuntimeException.class,
        () -> subject.findAllPossibleTransfers(transitLegs)
      );
    }
  }

  @Test
  void findDependedTransfersForThreeRoutes() {
    TestRoute l1 = route("L1", STOP_A, STOP_B, STOP_C).withTimetable(schedule("10:02 10:10 10:20"));
    TestRoute l2 = route("L2", STOP_B, STOP_D, STOP_E).withTimetable(schedule("10:12 10:22 10:32"));
    TestRoute l3 = route("L3", STOP_F, STOP_E, STOP_G).withTimetable(schedule("10:24 10:34 10:45"));

    data
      .withRoutes(l1, l2, l3)
      .withTransfer(STOP_C, TestTransfers.transfer(STOP_D, D30s))
      .withTransfer(STOP_D, TestTransfers.transfer(STOP_F, D20s));

    // The only possible place to transfer between A and D is stop C (no extra transfers):
    var path = pathBuilder
      .access(ACCESS_START, STOP_A, ACCESS_DURATION)
      .bus(l1.getTripSchedule(0), STOP_B)
      .bus(l2.getTripSchedule(0), STOP_E)
      .walk(D20s, STOP_F)
      .bus(l3.getTripSchedule(0), STOP_G)
      .egress(D1m);

    var transitLegs = path.transitLegs().collect(Collectors.toList());

    var subject = new TransferGenerator<>(tsAdaptor, data);

    var result = subject.findAllPossibleTransfers(transitLegs);

    assertEquals(
      "[[" +
      "TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:12 BUS L2]}, " +
      "TripToTripTransfer{from: [3 10:20 BUS L1], to: [4 10:22 BUS L2], transfer: WALK 30s $60 ~ 4}" +
      "], [" +
      "TripToTripTransfer{from: [4 10:22 BUS L2], to: [6 10:24 BUS L3], transfer: WALK 20s $40 ~ 6}, " +
      "TripToTripTransfer{from: [5 10:32 BUS L2], to: [5 10:34 BUS L3]}" +
      "]]",
      result.toString()
    );
  }

  private void testThatThereIsNoTransferAtStopB(TransferConstraint transfer) {
    // given 3 possible expected transfers
    var expBxB = "TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:20 BUS L2]}";
    var expCxD =
      "TripToTripTransfer{from: [3 10:20 BUS L1], to: [4 10:30 BUS L2], transfer: WALK 1m $120 ~ 4}";
    var expExE = "TripToTripTransfer{from: [5 10:30 BUS L1], to: [5 10:40 BUS L2]}";

    var l1 = route("L1", STOP_A, STOP_B, STOP_C, STOP_E)
      .withTimetable(schedule("10:00 10:10 10:20 10:30"));

    var l2 = route("L2", STOP_B, STOP_D, STOP_E, STOP_F)
      .withTimetable(schedule("10:20 10:30 10:40 10:50"));

    data.withRoutes(l1, l2).withTransfer(STOP_C, TestTransfers.transfer(STOP_D, D1m));

    final RaptorPath<TestTripSchedule> path = pathBuilder
      .access(ACCESS_START, STOP_A, ACCESS_DURATION)
      .bus(l1.getTripSchedule(0), STOP_C)
      .walk(D1m, STOP_D)
      .bus(l2.getTripSchedule(0), STOP_F)
      .egress(D1m);

    var transitLegs = path.transitLegs().collect(Collectors.toList());
    var subject = new TransferGenerator<>(tsAdaptor, data);

    var tripA = l1.getTripSchedule(0);
    var tripB = l2.getTripSchedule(0);

    data.withConstrainedTransfer(tripA, STOP_B, tripB, STOP_B, transfer);
    var result = subject.findAllPossibleTransfers(transitLegs);

    // The same stop transfer is no longer an option
    assertEquals("[[" + expCxD + ", " + expExE + "]]", result.toString());

    data.clearConstrainedTransfers();
    data.withConstrainedTransfer(tripA, STOP_C, tripB, STOP_D, transfer);
    result = subject.findAllPossibleTransfers(transitLegs);

    // The same stop transfer is no longer an option
    assertEquals("[[" + expBxB + ", " + expExE + "]]", result.toString());
  }

  private List<TransitPathLeg<TestTripSchedule>> transitLegsTwoRoutes(
    int accessStop,
    int transferStop,
    int egressStop
  ) {
    var schedule1 = data.getRoute(0).getTripSchedule(0);
    var schedule2 = data.getRoute(1).getTripSchedule(0);
    return transitLegs(schedule1, schedule2, accessStop, transferStop, egressStop);
  }

  private List<TransitPathLeg<TestTripSchedule>> transitLegsSameRoute(
    int accessStop,
    int transferStop,
    int egressStop
  ) {
    var schedule1 = data.getRoute(0).getTripSchedule(0);
    var schedule2 = data.getRoute(0).getTripSchedule(1);
    return transitLegs(schedule1, schedule2, accessStop, transferStop, egressStop);
  }

  private List<TransitPathLeg<TestTripSchedule>> transitLegs(
    TestTripSchedule schedule1,
    TestTripSchedule schedule2,
    int accessStop,
    int transferStop,
    int egressStop
  ) {
    var path = pathBuilder
      .access(ACCESS_START, accessStop, ACCESS_DURATION)
      .bus(schedule1, transferStop)
      .bus(schedule2, egressStop)
      .egress(D1m);

    return path.transitLegs().collect(Collectors.toList());
  }
}
