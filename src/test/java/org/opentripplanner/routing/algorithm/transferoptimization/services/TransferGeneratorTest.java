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
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.api.PathBuilder;
import org.opentripplanner.transit.raptor._data.transit.TestRoute;
import org.opentripplanner.transit.raptor._data.transit.TestTransfer;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.util.time.TimeUtils;

class TransferGeneratorTest implements RaptorTestConstants {

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

    private final PathBuilder pathBuilder = new PathBuilder(ALIGHT_SLACK, COST_CALCULATOR);

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
                "[[TripToTripTransfer{from: [2 10:10:00 BUS L1], to: [2 10:12:00 BUS L1]}]]",
                subject.findAllPossibleTransfers(transitLegs).toString()
        );

        // The only possible place to transfer between B and D is stop C:
        transitLegs = transitLegsSameRoute(STOP_B, STOP_C, STOP_D);
        subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);
        assertEquals(
                "[[TripToTripTransfer{from: [3 10:20:00 BUS L1], to: [3 10:22:00 BUS L1]}]]",
                subject.findAllPossibleTransfers(transitLegs).toString()
        );

        // Between A and D transfers may happen at stop B and C. The transfers should be sorted on
        // the to-trip-departure-time (descending)
        transitLegs = transitLegsSameRoute(STOP_A, STOP_C, STOP_D);
        subject = new TransferGenerator<>(TS_ADAPTOR, SLACK_PROVIDER, data);
        assertEquals(
                "[[TripToTripTransfer{from: [2 10:10:00 BUS L1], to: [2 10:12:00 BUS L1]}, "
                        + "TripToTripTransfer{from: [3 10:20:00 BUS L1], to: [3 10:22:00 BUS L1]}]]",
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
                        + "TripToTripTransfer{from: [2 10:20:00 BUS L1], to: [2 10:20:00 BUS L2]}"
                        + "], ["
                        + "TripToTripTransfer{from: [3 10:30:00 BUS L2], to: [4 10:31:00 BUS L3], transfer: Walk 1m ~ 4}"
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
                        + "TripToTripTransfer{from: [2 10:10:00 BUS L1], to: [5 10:12:00 BUS L2], transfer: Walk 1m ~ 5}, "
                        + "TripToTripTransfer{from: [3 10:20:00 BUS L1], to: [3 10:22:00 BUS L2]}, "
                        + "TripToTripTransfer{from: [4 10:30:00 BUS L1], to: [6 10:32:00 BUS L2], transfer: Walk 20s ~ 6}"
                        + "]]",
                result.toString()
        );
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
                        + "TripToTripTransfer{from: [2 10:10:00 BUS L1], to: [2 10:12:00 BUS L2]}, "
                        + "TripToTripTransfer{from: [3 10:20:00 BUS L1], to: [4 10:22:00 BUS L2], transfer: Walk 30s ~ 4}"
                        + "], ["
                        + "TripToTripTransfer{from: [4 10:22:00 BUS L2], to: [6 10:24:00 BUS L3], transfer: Walk 20s ~ 6}, "
                        + "TripToTripTransfer{from: [5 10:32:00 BUS L2], to: [5 10:34:00 BUS L3]}"
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