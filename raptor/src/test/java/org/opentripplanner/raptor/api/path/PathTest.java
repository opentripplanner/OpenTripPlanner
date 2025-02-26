package org.opentripplanner.raptor.api.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.ACCESS_START;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_DETAILED_STRING;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_STRING;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.EGRESS_END;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.RAPTOR_ITERATION_START_TIME;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.TOTAL_C1;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.basicTripStops;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.utils.time.TimeUtils.time;
import static org.opentripplanner.utils.time.TimeUtils.timeToStrCompact;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransferConstraint;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.path.Path;

public class PathTest implements RaptorTestConstants {

  private final RaptorPath<TestTripSchedule> subject = BasicPathTestCase.basicTripAsPath();

  @Test
  public void rangeRaptorIterationDepartureTime() {
    assertEquals(RAPTOR_ITERATION_START_TIME, subject.rangeRaptorIterationDepartureTime());
  }

  @Test
  public void startTime() {
    assertEquals(ACCESS_START, subject.startTime());
  }

  @Test
  public void endTime() {
    assertEquals(EGRESS_END, subject.endTime());
  }

  @Test
  public void totalTravelDurationInSeconds() {
    assertEquals("1:59:45", timeToStrCompact(subject.durationInSeconds()));
  }

  @Test
  public void numberOfTransfers() {
    assertEquals(2, subject.numberOfTransfers());
    assertEquals(2, subject.numberOfTransfersExAccessEgress());
  }

  @Test
  public void accessLeg() {
    assertNotNull(subject.accessLeg());
  }

  @Test
  public void egressLeg() {
    assertNotNull(subject.egressLeg());
  }

  @Test
  public void legStream() {
    assertEquals(6, subject.legStream().count());
  }

  @Test
  public void transitLegs() {
    assertEquals(3, subject.transitLegs().count());
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  public void nextTransitLeg() {
    TransitPathLeg<?> leg = subject.accessLeg().nextTransitLeg();
    assertEquals("BUS L11 10:04-10:35(31m) ~ 2", leg.toString());

    leg = leg.nextTransitLeg();
    assertEquals("BUS L21 11:00-11:23(23m) ~ 4", leg.toString());

    leg = leg.nextTransitLeg();
    assertEquals("BUS L31 11:40-11:52(12m) ~ 5", leg.toString());

    leg = leg.nextTransitLeg();
    assertNull(leg);
  }

  @Test
  public void listStops() {
    assertEquals(basicTripStops(), subject.listStops());
  }

  @Test
  public void cost() {
    assertEquals(TOTAL_C1, subject.c1());
  }

  @Test
  public void waitTime() {
    assertEquals(time("0:39:15"), subject.waitTime());
  }

  @Test
  public void testToString() {
    assertEquals(BASIC_PATH_AS_STRING, subject.toString(RaptorTestConstants::stopIndexToName));
  }

  @Test
  public void testToStringDetailed() {
    assertEquals(
      BASIC_PATH_AS_DETAILED_STRING,
      subject.toStringDetailed(RaptorTestConstants::stopIndexToName)
    );
  }

  @Test
  public void equals() {
    assertEquals(BasicPathTestCase.basicTripAsPath(), subject);
  }

  @Test
  public void testHashCode() {
    var expected = BasicPathTestCase.basicTripAsPath();
    assertEquals(expected.hashCode(), subject.hashCode());
  }

  @Test
  public void testCompareTo() {
    var p0 = Path.dummyPath(0, 10, 20, 10, 10);
    var p1 = Path.dummyPath(0, 11, 20, 10, 10);
    var p2 = Path.dummyPath(0, 10, 19, 10, 10);
    var p3 = Path.dummyPath(0, 10, 20, 9, 10);
    var p4 = Path.dummyPath(0, 10, 20, 10, 9);

    // Order: < EndTime, > StartTime, < Cost, < Transfers
    List<RaptorPath<?>> expected = List.of(p2, p1, p4, p3, p0);

    List<RaptorPath<?>> paths = Stream.of(p4, p3, p2, p1, p0).sorted().collect(Collectors.toList());

    assertEquals(expected, paths);
  }

  @Test
  public void testCountTransfersWithStaySeated() {
    int egressStart = time("09:30");
    int egressEnd = time("09:40");
    RaptorAccessEgress egress = TestAccessEgress.walk(STOP_C, egressEnd - egressStart);
    PathLeg<TestTripSchedule> leg4 = new EgressPathLeg<>(
      egress,
      egressStart,
      egressEnd,
      egress.c1()
    );

    var trip3 = TestTripSchedule
      .schedule(pattern("L1", STOP_B, STOP_C))
      .times(time("09:20"), egressStart)
      .build();
    TransitPathLeg<TestTripSchedule> leg3 = new TransitPathLeg<>(
      trip3,
      trip3.departure(0),
      trip3.arrival(1),
      0,
      1,
      null,
      600,
      leg4
    );

    var trip2 = TestTripSchedule
      .schedule(pattern("L1", STOP_A, STOP_B))
      .times(time("09:10"), time("09:20"))
      .build();

    var tx = TestTransferConstraint.staySeated();
    TransitPathLeg<TestTripSchedule> leg2 = new TransitPathLeg<>(
      trip2,
      trip2.departure(0),
      trip2.arrival(1),
      0,
      1,
      () -> tx,
      600,
      leg3
    );

    int accessStart = time("09:00");
    int accessEnd = time("09:10");
    RaptorAccessEgress access = TestAccessEgress.walk(STOP_A, accessEnd - accessStart);
    AccessPathLeg<TestTripSchedule> leg1 = new AccessPathLeg<>(
      access,
      accessStart,
      accessEnd,
      access.c1(),
      leg2.asTransitLeg()
    );
    RaptorPath<TestTripSchedule> path = new Path<>(accessStart, leg1, TOTAL_C1, 0);
    assertEquals(0, path.numberOfTransfers());
  }

  @Test
  public void testCountTransfersWithTransfer() {
    int egressStart = time("09:30");
    int egressEnd = time("09:40");
    RaptorAccessEgress egress = TestAccessEgress.walk(STOP_C, egressEnd - egressStart);
    PathLeg<TestTripSchedule> leg4 = new EgressPathLeg<>(
      egress,
      egressStart,
      egressEnd,
      egress.c1()
    );

    var trip3 = TestTripSchedule
      .schedule(pattern("L1", STOP_B, STOP_C))
      .times(time("09:20"), egressStart)
      .build();
    TransitPathLeg<TestTripSchedule> leg3 = new TransitPathLeg<>(
      trip3,
      trip3.departure(0),
      trip3.arrival(1),
      0,
      1,
      null,
      600,
      leg4
    );

    var trip2 = TestTripSchedule
      .schedule(pattern("L1", STOP_A, STOP_B))
      .times(time("09:10"), time("09:20"))
      .build();

    TransitPathLeg<TestTripSchedule> leg2 = new TransitPathLeg<>(
      trip2,
      trip2.departure(0),
      trip2.arrival(1),
      0,
      1,
      null,
      600,
      leg3
    );

    int accessStart = time("09:00");
    int accessEnd = time("09:10");
    RaptorAccessEgress access = TestAccessEgress.walk(STOP_A, accessEnd - accessStart);
    AccessPathLeg<TestTripSchedule> leg1 = new AccessPathLeg<>(
      access,
      accessStart,
      accessEnd,
      access.c1(),
      leg2.asTransitLeg()
    );
    RaptorPath<TestTripSchedule> path = new Path<>(accessStart, leg1, TOTAL_C1, 0);
    assertEquals(1, path.numberOfTransfers());
  }
}
