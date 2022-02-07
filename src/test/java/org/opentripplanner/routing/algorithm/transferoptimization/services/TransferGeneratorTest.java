package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.COST_CALCULATOR;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.api.TestPathBuilder;
import org.opentripplanner.transit.raptor._data.transit.TestRoute;
import org.opentripplanner.transit.raptor._data.transit.TestTransfer;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripPattern;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.util.time.TimeUtils;

public class TransferGeneratorTest implements RaptorTestConstants {

    // Given a total slack of 30 seconds
    private static final int BOARD_SLACK = 10;
    private static final int TRANSFER_SLACK = 15;
    private static final int ALIGHT_SLACK = 5;
    // Access walk start 1 minute before departure
    private static final int ACCESS_START = TimeUtils.time("10:00");
    private static final int ACCESS_DURATION = D1m;

    private static final RaptorSlackProvider SLACK_PROVIDER = defaultSlackProvider(
            TRANSFER_SLACK, BOARD_SLACK, ALIGHT_SLACK
    );

    private final TestPathBuilder pathBuilder = new TestPathBuilder(ALIGHT_SLACK, COST_CALCULATOR);

    private final TestTransitData data = new TestTransitData();

    private final TransferServiceAdaptor<TestTripSchedule> TS_ADAPTOR = data.transferServiceAdaptor();

    @Test
    void findTransferPathWithoutTransfers() {
        data.withRoutes(
                route("L1", STOP_A, STOP_B, STOP_C)
                .withTimetable(schedule("10:00 10:20 10:30"))

        );
        var schedule = data.getRoute(0).getTripSchedule(0);

        var path = pathBuilder
                .access(ACCESS_START, ACCESS_DURATION, STOP_A)
                .bus(schedule, STOP_C)
                .egress(D1m);

        var transitLegs = path.transitLegs().collect(Collectors.toList());

        var subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);

        assertEquals("[]", subject.findAllPossibleTransfers(transitLegs).toString()
        );
    }

    @Test
    void findTransferForTheSameRoute() {
        data.withRoutes(
                route("L1", STOP_A, STOP_B, STOP_C, STOP_D)
                .withTimetable(
                        schedule("10:02 10:10 10:20 10:30"),
                        schedule("10:04 10:12 10:22 10:32")
                )
        );

        // The only possible place to transfer between A and C is stop B:
        var transitLegs = transitLegsSameRoute(STOP_A, STOP_B, STOP_C);
        var subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);
        assertEquals(
                "[[TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:12 BUS L1]}]]",
                subject.findAllPossibleTransfers(transitLegs).toString()
        );

        // The only possible place to transfer between B and D is stop C:
        transitLegs = transitLegsSameRoute(STOP_B, STOP_C, STOP_D);
        subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);
        assertEquals(
                "[[TripToTripTransfer{from: [3 10:20 BUS L1], to: [3 10:22 BUS L1]}]]",
                subject.findAllPossibleTransfers(transitLegs).toString()
        );

        // Between A and D transfers may happen at stop B and C. The transfers should be sorted on
        // the to-trip-departure-time (descending)
        transitLegs = transitLegsSameRoute(STOP_A, STOP_C, STOP_D);
        subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);
        assertEquals(
                "[[TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:12 BUS L1]}, "
                        + "TripToTripTransfer{from: [3 10:20 BUS L1], to: [3 10:22 BUS L1]}]]",
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
            .withTransfer(STOP_C, TestTransfer.walk(STOP_D, D1m))
            .withGuaranteedTransfer(schedule1, STOP_B, schedule2, STOP_B)
            .withGuaranteedTransfer(schedule2, STOP_C, schedule3, STOP_D)
        ;

        var path = pathBuilder
                .access(ACCESS_START, ACCESS_DURATION, STOP_A)
                .bus(schedule1, STOP_B)
                .bus(schedule2, STOP_C)
                .walk(D1m, STOP_D)
                .bus(schedule3, STOP_E)
                .egress(D1m);

        var transitLegs = path.transitLegs().collect(Collectors.toList());

        var subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);

        var result = subject.findAllPossibleTransfers(transitLegs);

        assertEquals(
                "[["
                        + "TripToTripTransfer{from: [2 10:20 BUS L1], to: [2 10:20 BUS L2]}"
                        + "], ["
                        + "TripToTripTransfer{from: [3 10:30 BUS L2], to: [4 10:31 BUS L3], transfer: On-Street 1m ~ 4}"
                        + "]]",
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
        data.withRoutes(l1, l2)
            .withTransfer(STOP_B, walk(STOP_E, D1m))
            .withTransfer(STOP_D, walk(STOP_F, D20s));

        // The only possible place to transfer between A and D is stop C (no extra transfers):
        var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_C, STOP_G);

        var subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);

        var result = subject.findAllPossibleTransfers(transitLegs);
        assertEquals(
                "[["
                        + "TripToTripTransfer{from: [2 10:10 BUS L1], to: [5 10:12 BUS L2], transfer: On-Street 1m ~ 5}, "
                        + "TripToTripTransfer{from: [3 10:20 BUS L1], to: [3 10:22 BUS L2]}, "
                        + "TripToTripTransfer{from: [4 10:30 BUS L1], to: [6 10:32 BUS L2], transfer: On-Street 20s ~ 6}"
                        + "]]",
                result.toString()
        );
    }

    @Test
    void findTransferForDifferentRoutesWithCustomBoardingSlack() {
        TestRoute l1 = route("L1", STOP_A, STOP_B)
                .withTimetable(schedule("10:00 10:10"));
        TestRoute l2 = route("L2", STOP_B, STOP_C)
                .withTimetable(schedule("10:20 10:30"));

        // S
        data.withRoutes(l1, l2);

        // The only possible place to transfer between A and D is stop C (no extra transfers):
        var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_B, STOP_C);

        RaptorSlackProvider slackProvider = new RaptorSlackProvider() {
            @Override public int transferSlack() { return 0; }
            @Override public int boardSlack(RaptorTripPattern pattern) {
                return ((TestTripPattern) pattern).getName().equals("L1") ? 20 * 60 : 0;
            }
            @Override public int alightSlack(RaptorTripPattern pattern) { return 0; }
        };

        var subject = new TransferGenerator<>(TS_ADAPTOR, slackProvider, data);

        var result = subject.findAllPossibleTransfers(transitLegs);
        assertEquals(
                "[["
                        + "TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:20 BUS L2]}"
                        + "]]",
                result.toString()
        );
    }


    @Test
    void findTransferWithAlightingForbiddenAtSameStop() {
        TestTripPattern p1 = TestTripPattern.pattern("L1", STOP_A, STOP_B, STOP_C);
        p1.restrictions("B B A");

        TestRoute l1 = route(p1)
                .withTimetable(schedule("10:00 10:10 10:20"));
        TestRoute l2 = route("L2", STOP_B, STOP_D, STOP_E)
                .withTimetable(schedule("10:20 10:30 10:40"));

        data.withRoutes(l1, l2).withTransfer(STOP_C, walk(STOP_D, D1m));

        final Path<TestTripSchedule> path = pathBuilder
                .access(ACCESS_START, ACCESS_DURATION, STOP_A)
                .bus(l1.getTripSchedule(0), STOP_C)
                .walk(D1m, STOP_D)
                .bus(l2.getTripSchedule(0), STOP_E)
                .egress(D1m);

        var transitLegs = path.transitLegs().collect(Collectors.toList());

        var subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);

        var result = subject.findAllPossibleTransfers(transitLegs);

        // Transfer at B is not allowed
        assertEquals(
                "[["
                        + "TripToTripTransfer{from: [3 10:20 BUS L1], to: [4 10:30 BUS L2], transfer: On-Street 1m ~ 4}"
                        + "]]",
                result.toString()
        );
    }

    @Test
    void findTransferWithBoardingForbiddenAtSameStop() {
        TestRoute l1 = route("L1", STOP_A, STOP_B, STOP_C)
                .withTimetable(schedule("10:00 10:10 10:20"));

        TestTripPattern p2 = TestTripPattern.pattern("L2", STOP_B, STOP_D, STOP_E);
        p2.restrictions("A BA A");

        TestRoute l2 = route(p2)
                .withTimetable(schedule("10:20 10:30 10:40"));

        data.withRoutes(l1, l2).withTransfer(STOP_C, walk(STOP_D, D1m));

        final Path<TestTripSchedule> path = pathBuilder
                .access(ACCESS_START, ACCESS_DURATION, STOP_A)
                .bus(l1.getTripSchedule(0), STOP_C)
                .walk(D1m, STOP_D)
                .bus(l2.getTripSchedule(0), STOP_E)
                .egress(D1m);

        var transitLegs = path.transitLegs().collect(Collectors.toList());

        var subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);

        var result = subject.findAllPossibleTransfers(transitLegs);

        // Transfer at B is not allowed
        assertEquals(
                "[["
                        + "TripToTripTransfer{from: [3 10:20 BUS L1], to: [4 10:30 BUS L2], transfer: On-Street 1m ~ 4}"
                        + "]]",
                result.toString()
        );
    }

    @Test
    void findTransferWithAlightingForbiddenAtDifferentStop() {
        TestTripPattern p1 = TestTripPattern.pattern("L1", STOP_A, STOP_B, STOP_C);
        p1.restrictions("B A B");

        TestRoute l1 = route(p1)
                .withTimetable(schedule("10:00 10:10 10:20"));
        TestRoute l2 = route("L2", STOP_B, STOP_D, STOP_E)
                .withTimetable(schedule("10:20 10:30 10:40"));

        data.withRoutes(l1, l2).withTransfer(STOP_C, walk(STOP_D, D1m));

        var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_B, STOP_E);

        var subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);

        var result = subject.findAllPossibleTransfers(transitLegs);

        // Transfer at C is not allowed
        assertEquals(
                "[["
                        + "TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:20 BUS L2]}"
                        + "]]",
                result.toString()
        );
    }

    @Test
    void findTransferWithBoardingForbiddenAtDifferentStop() {
        TestRoute l1 = route("L1", STOP_A, STOP_B, STOP_C)
                .withTimetable(schedule("10:00 10:10 10:20"));

        TestTripPattern p2 = TestTripPattern.pattern("L2", STOP_B, STOP_D, STOP_E);
        p2.restrictions("B A A");

        TestRoute l2 = route(p2)
                .withTimetable(schedule("10:20 10:30 10:40"));

        data.withRoutes(l1, l2).withTransfer(STOP_C, walk(STOP_D, D1m));

        var transitLegs = transitLegsTwoRoutes(STOP_A, STOP_B, STOP_E);

        var subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);

        var result = subject.findAllPossibleTransfers(transitLegs);

        // Transfer at D is not allowed
        assertEquals(
                "[["
                        + "TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:20 BUS L2]}"
                        + "]]",
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

    private void testThatThereIsNoTransferAtStopB(TransferConstraint transfer) {
        // given 3 possible expected transfers
        var expBxB = "TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:20 BUS L2]}";
        var expCxD = "TripToTripTransfer{from: [3 10:20 BUS L1], to: [4 10:30 BUS L2], transfer: On-Street 1m ~ 4}";
        var expExE = "TripToTripTransfer{from: [5 10:30 BUS L1], to: [5 10:40 BUS L2]}";

        var l1 = route("L1", STOP_A, STOP_B, STOP_C, STOP_E)
                .withTimetable(schedule("10:00 10:10 10:20 10:30"));

        var l2 = route("L2", STOP_B, STOP_D, STOP_E, STOP_F)
                .withTimetable(schedule("10:20 10:30 10:40 10:50"));

        data.withRoutes(l1, l2).withTransfer(STOP_C, walk(STOP_D, D1m));

        final Path<TestTripSchedule> path = pathBuilder
                .access(ACCESS_START, ACCESS_DURATION, STOP_A)
                .bus(l1.getTripSchedule(0), STOP_C)
                .walk(D1m, STOP_D)
                .bus(l2.getTripSchedule(0), STOP_F)
                .egress(D1m);

        var transitLegs = path.transitLegs().collect(Collectors.toList());
        var subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);

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

    @Test
    void findDependedTransfersForThreeRoutes() {
        TestRoute l1 = route("L1", STOP_A, STOP_B, STOP_C)
                .withTimetable(schedule("10:02 10:10 10:20"));
        TestRoute l2 = route("L2", STOP_B, STOP_D, STOP_E)
                .withTimetable(schedule("10:12 10:22 10:32"));
        TestRoute l3 = route("L3", STOP_F, STOP_E, STOP_G)
                .withTimetable(schedule("10:24 10:34 10:45"));

        data.withRoutes(l1, l2, l3)
                .withTransfer(STOP_C, walk(STOP_D, D30s))
                .withTransfer(STOP_D, walk(STOP_F, D20s));

        // The only possible place to transfer between A and D is stop C (no extra transfers):
        var path = pathBuilder
                .access(ACCESS_START, ACCESS_DURATION, STOP_A)
                .bus(l1.getTripSchedule(0), STOP_B)
                .bus(l2.getTripSchedule(0), STOP_E)
                .walk(D20s, STOP_F)
                .bus(l3.getTripSchedule(0), STOP_G)
                .egress(D1m);

        var transitLegs = path.transitLegs().collect(Collectors.toList());

        var subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);

        var result = subject.findAllPossibleTransfers(transitLegs);

        assertEquals(
                "[["
                        + "TripToTripTransfer{from: [2 10:10 BUS L1], to: [2 10:12 BUS L2]}, "
                        + "TripToTripTransfer{from: [3 10:20 BUS L1], to: [4 10:22 BUS L2], transfer: On-Street 30s ~ 4}"
                        + "], ["
                        + "TripToTripTransfer{from: [4 10:22 BUS L2], to: [6 10:24 BUS L3], transfer: On-Street 20s ~ 6}, "
                        + "TripToTripTransfer{from: [5 10:32 BUS L2], to: [5 10:34 BUS L3]}"
                        + "]]",
                result.toString()
        );
    }

    private List<TransitPathLeg<TestTripSchedule>> transitLegsTwoRoutes(
            int accessStop, int transferStop, int egressStop
    ) {
        var schedule1 = data.getRoute(0).getTripSchedule(0);
        var schedule2 = data.getRoute(1).getTripSchedule(0);
        return transitLegs(schedule1, schedule2, accessStop, transferStop, egressStop);
    }

    private List<TransitPathLeg<TestTripSchedule>> transitLegsSameRoute(
            int accessStop, int transferStop, int egressStop
    ) {
        var schedule1 = data.getRoute(0).getTripSchedule(0);
        var schedule2 = data.getRoute(0).getTripSchedule(1);
        return transitLegs(schedule1, schedule2, accessStop, transferStop, egressStop);
    }

    private List<TransitPathLeg<TestTripSchedule>> transitLegs(
            TestTripSchedule schedule1, TestTripSchedule schedule2,
            int accessStop, int transferStop, int egressStop
    ) {
        var path = pathBuilder
                .access(ACCESS_START, ACCESS_DURATION, accessStop)
                .bus(schedule1, transferStop)
                .bus(schedule2, egressStop)
                .egress(D1m);

        return path.transitLegs().collect(Collectors.toList());
    }
}