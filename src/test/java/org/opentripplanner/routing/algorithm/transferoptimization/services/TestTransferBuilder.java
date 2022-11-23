package org.opentripplanner.routing.algorithm.transferoptimization.services;

import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This builder is used to create a {@link ConstrainedTransfer} for use in unit-tests. It build a
 * valid instance with dummy trip reference.
 */
@SuppressWarnings("UnusedReturnValue")
public class TestTransferBuilder<T extends RaptorTripSchedule> {

  private final T fromTrip;
  private final int fromStopIndex;
  private final T toTrip;
  private final int toStopIndex;
  private final TransferConstraint.Builder constraint = TransferConstraint.create();

  private TestTransferBuilder(T fromTrip, int fromStopIndex, T toTrip, int toStopIndex) {
    this.fromTrip = fromTrip;
    this.fromStopIndex = fromStopIndex;
    this.toTrip = toTrip;
    this.toStopIndex = toStopIndex;
  }

  public static <T extends RaptorTripSchedule> TestTransferBuilder<T> txConstrained(
    T fromTrip,
    int fromStopIndex,
    T toTrip,
    int toStopIndex
  ) {
    return new TestTransferBuilder<>(fromTrip, fromStopIndex, toTrip, toStopIndex);
  }

  public T getFromTrip() {
    return fromTrip;
  }

  public int getFromStopIndex() {
    return fromStopIndex;
  }

  public T getToTrip() {
    return toTrip;
  }

  public int getToStopIndex() {
    return toStopIndex;
  }

  public TestTransferBuilder<T> staySeated() {
    this.constraint.staySeated();
    return this;
  }

  public TestTransferBuilder<T> guaranteed() {
    this.constraint.guaranteed();
    return this;
  }

  public TestTransferBuilder<T> maxWaitTime(int maxWaitTime) {
    this.constraint.maxWaitTime(maxWaitTime);
    return this;
  }

  public TestTransferBuilder<T> priority(TransferPriority priority) {
    this.constraint.priority(priority);
    return this;
  }

  public ConstrainedTransfer build() {
    if (fromTrip == null) {
      throw new NullPointerException();
    }
    if (toTrip == null) {
      throw new NullPointerException();
    }

    int fromStopPos = fromTrip.pattern().findStopPositionAfter(0, fromStopIndex);
    int toStopPos = toTrip.pattern().findStopPositionAfter(0, toStopIndex);

    return new ConstrainedTransfer(
      null,
      new TripTransferPoint(createDummyTrip(fromTrip), fromStopPos),
      new TripTransferPoint(createDummyTrip(toTrip), toStopPos),
      constraint.build()
    );
  }

  private static <T extends RaptorTripSchedule> Trip createDummyTrip(T trip) {
    // Set a uniq id: pattern + the first stop departure time
    return TransitModelForTest
      .trip(trip.pattern().debugInfo() + ":" + TimeUtils.timeToStrCompact(trip.departure(0)))
      .build();
  }
}
