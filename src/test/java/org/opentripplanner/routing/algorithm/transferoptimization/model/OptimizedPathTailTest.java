package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TestTransferBuilder.txConstrained;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;

class OptimizedPathTailTest implements RaptorTestConstants {
    private final Path<TestTripSchedule> orgPath = BasicPathTestCase.basicTripAsPath();

    private final TransitPathLeg<TestTripSchedule> t1 = orgPath.accessLeg().nextTransitLeg();
    @SuppressWarnings("ConstantConditions")
    private final TransitPathLeg<TestTripSchedule> t2 = t1.nextTransitLeg();
    @SuppressWarnings("ConstantConditions")
    private final TransitPathLeg<TestTripSchedule> t3 = t2.nextTransitLeg();
    private final TripToTripTransfer<TestTripSchedule> tx12 = TransferGeneratorDummy.tx(
            t1.trip(), STOP_B, D2m, STOP_C, t2.trip()
    );
    @SuppressWarnings("ConstantConditions")
    private final TripToTripTransfer<TestTripSchedule> tx23 = TransferGeneratorDummy.tx(
            txConstrained(t2.trip(), STOP_D, t3.trip(), STOP_D).staySeated()
    );

    private final TransferWaitTimeCalculator waitTimeCalc = new TransferWaitTimeCalculator(1.0, 5.0);

    private final OptimizedPathTail<TestTripSchedule> subject = new OptimizedPathTail<>(
            BasicPathTestCase.SLACK_PROVIDER,
            BasicPathTestCase.COST_CALCULATOR,
            waitTimeCalc,
            this::stopIndexToName
    );


    @BeforeEach
    void setup() {
        waitTimeCalc.setMinSafeTransferTime(D5m);
    }

    @Test
    void testToSting() {
        subject.addTransitTail(t3);
        subject.addTransitAndTransferLeg(t2, tx23);
        subject.addTransitAndTransferLeg(t1, tx12);
        subject.access(orgPath.accessLeg().access());

        var exp = "Walk 3m15s ~ A "
                + "~ BUS L11 10:04 10:35 ~ B "
                + "~ Walk 2m ~ C "
                + "~ BUS L21 11:00 11:23 ~ D "
                + "~ BUS L31 11:40 11:52 ~ E "
                + "~ Walk 7m45s "
                + "[$8019 $46pri $-101226.08wtc]";

        assertEquals(exp, subject.toString());
    }


    @Test
    void testMutate() {

        OptimizedPathTail<TestTripSchedule> copy;

        subject.addTransitTail(t3);

        copy = subject.mutate();
        assertEquals(subject.toString(), copy.toString());

        subject.addTransitAndTransferLeg(t2, tx23);

        copy = subject.mutate();
        assertEquals(subject.toString(), copy.toString());

        subject.addTransitAndTransferLeg(t1, tx12);

        copy = subject.mutate();
        assertEquals(subject.toString(), copy.toString());

        subject.access(orgPath.accessLeg().access());

        copy = subject.mutate();
        assertEquals(subject.toString(), copy.toString());
    }

    @Test
    void testBuildingPath() {
        subject.addTransitTail(t3);
        subject.addTransitAndTransferLeg(t2, tx23);
        subject.addTransitAndTransferLeg(t1, tx12);
        subject.access(orgPath.accessLeg().access());

        var path = subject.build(0);

        // We have replaced the first transfer with a 2 minute walk
        var expPath = "Walk 3m15s 10:00 10:03:15 $390 ~ A 45s "
                + "~ BUS L11 10:04 10:35 31m $1998 ~ B 15s "
                + "~ Walk 2m 10:35:15 10:37:15 $240 ~ C 22m45s "
                + "~ BUS L21 11:00 11:23 23m $2724 ~ D 17m {staySeated} "
                + "~ BUS L31 11:40 11:52 12m $1737 ~ E 15s "
                + "~ Walk 7m45s 11:52:15 12:00 $930 "
                + "[10:00 12:00 2h $8019 $46pri $-101226.08wtc]";

        assertEquals(expPath, path.toStringDetailed(this::stopIndexToName));
    }
}