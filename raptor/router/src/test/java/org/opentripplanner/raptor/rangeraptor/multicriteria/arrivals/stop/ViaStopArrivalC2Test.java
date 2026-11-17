package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.raptor.api.view.PathLegType.TRANSFER;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

class ViaStopArrivalC2Test {

  private static final int ACCESS_DEPARTURE_TIME = 8 * 60 * 60;
  private static final int TRANSIT_ALIGHT_TIME = 9 * 60 * 60;
  private static final int TRANSIT_C1 = 128000;
  private static final int TRANSIT_C2 = 6;
  private static final int TRANSFER_TO_STOP = 102;
  private static final int TRANSFER_DURATION = 360;
  private static final int MIN_WAIT_TIME = 120;
  private static final TestTransfer VIA_TRANSFER = TestTransfer.transfer(
    TRANSFER_TO_STOP,
    TRANSFER_DURATION
  );

  private static final TransitStopArrivalC2<RaptorTripSchedule> TRANSIT_ARRIVAL =
    new TransitStopArrivalC2<>(
      new AccessStopArrivalC2<>(ACCESS_DEPARTURE_TIME, TestAccessEgress.walk(100, 300)),
      101,
      TRANSIT_ALIGHT_TIME,
      TRANSIT_C1,
      TRANSIT_C2,
      0,
      null
    );

  private static final int REGULAR_TRANSFER_C1 = 720;

  // The arrival time includes the minimum wait time of the via connection
  private final ViaStopArrivalC2<RaptorTripSchedule> subject = ViaStopArrivalC2.ofViaTransfer(
    TRANSIT_ARRIVAL,
    VIA_TRANSFER,
    TRANSIT_ALIGHT_TIME + TRANSFER_DURATION + MIN_WAIT_TIME
  );

  @Test
  void arrivedBy() {
    assertEquals(TRANSFER, subject.arrivedBy());
  }

  @Test
  void viaTransferIsKept() {
    assertSame(VIA_TRANSFER, subject.viaTransfer());
  }

  @Test
  void c2IsTheSameAsForThePreviousArrival() {
    assertEquals(TRANSIT_C2, subject.c2());
  }

  @Test
  void stopAndC1() {
    assertEquals(TRANSFER_TO_STOP, subject.stop());
    assertEquals(TRANSIT_C1 + VIA_TRANSFER.c1(), subject.c1());
    assertEquals(VIA_TRANSFER.c1(), subject.transferC1());
  }

  @Test
  void hopDurationIncludesTheMinimumWaitTime() {
    assertEquals(TRANSFER_DURATION + MIN_WAIT_TIME, subject.hopDuration());
    assertEquals(TRANSFER_DURATION, subject.viaTransfer().durationInSeconds());
  }

  @Test
  void addSlackKeepsTheViaTransfer() {
    var withSlack = subject.addSlackToArrivalTime(15);

    assertNotSame(subject, withSlack);
    assertEquals(subject.arrivalTime() + 15, withSlack.arrivalTime());
    assertSame(VIA_TRANSFER, withSlack.viaTransfer());
    assertEquals(subject.c1(), withSlack.c1());
    assertSame(TRANSIT_ARRIVAL, withSlack.previous());
  }

  private final TransferStopArrivalC2<RaptorTripSchedule> regularTransfer =
    new TransferStopArrivalC2<>(
      TRANSIT_ARRIVAL,
      TRANSFER_TO_STOP,
      TRANSIT_ALIGHT_TIME + TRANSFER_DURATION,
      REGULAR_TRANSFER_C1
    );

  @Test
  void aRegularTransferHasNoViaWaitTime() {
    assertEquals(0, regularTransfer.viaWaitTime());
    assertEquals(TRANSFER_DURATION, regularTransfer.hopDuration());
  }

  @Test
  void addSlackToARegularTransferKeepsTheTransferAndRemembersTheWaitTime() {
    var slacked = regularTransfer.addSlackToArrivalTime(120);

    assertInstanceOf(ViaStopArrivalC2.class, slacked);
    assertEquals(TRANSFER, slacked.arrivedBy());
    assertEquals(TRANSFER_TO_STOP, slacked.stop());
    assertEquals(regularTransfer.arrivalTime() + 120, slacked.arrivalTime());
    assertEquals(regularTransfer.c1(), slacked.c1());
    assertEquals(REGULAR_TRANSFER_C1, slacked.transferC1());
    assertSame(TRANSIT_ARRIVAL, slacked.previous());
    assertNull(slacked.viaTransfer());
    assertEquals(120, slacked.viaWaitTime());
    assertEquals(TRANSFER_DURATION + 120, slacked.hopDuration());
  }

  @Test
  void theWaitTimeIsTheSumIfTheArrivalIsAVisitOfSeveralViaLocations() {
    var slacked = regularTransfer.addSlackToArrivalTime(120).addSlackToArrivalTime(60);

    assertEquals(180, slacked.viaWaitTime());
    assertEquals(TRANSFER_DURATION + 180, slacked.hopDuration());
    assertEquals(REGULAR_TRANSFER_C1, slacked.transferC1());
  }
}
