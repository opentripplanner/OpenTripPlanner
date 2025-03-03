package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.Objects;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptorlegacy._data.transit.TestTransfers;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * This builder is used to create a {@link ConstrainedTransfer} for use in unit-tests. It build a
 * valid instance with dummy trip reference.
 */
@SuppressWarnings("UnusedReturnValue")
public class TestTransferBuilder<T extends RaptorTripSchedule> {

  private T fromTrip;
  private int fromStopIndex = RaptorConstants.NOT_SET;
  private T toTrip;
  private int toStopIndex = RaptorConstants.NOT_SET;

  // We set the default walk time to zero - it is not relevant for many tests and zero is easy
  private int walkDurationSec = 0;
  private TransferConstraint.Builder constraint = null;

  public static <T extends RaptorTripSchedule> TestTransferBuilder<T> tx(T fromTrip, T toTrip) {
    return new TestTransferBuilder<T>().fromTrip(fromTrip).toTrip(toTrip);
  }

  /**
   * Set all required parameters for a transfer. The walk duration is set to zero.
   */
  public static <T extends RaptorTripSchedule> TestTransferBuilder<T> tx(
    T fromTrip,
    int fromStopIndex,
    T toTrip,
    int toStopIndex
  ) {
    return tx(fromTrip, toTrip).fromStopIndex(fromStopIndex).toStopIndex(toStopIndex);
  }

  public static <T extends RaptorTripSchedule> TestTransferBuilder<T> tx(
    T fromTrip,
    int sameStopIndex,
    T toTrip
  ) {
    return tx(fromTrip, sameStopIndex, toTrip, sameStopIndex);
  }

  public static <T extends RaptorTripSchedule> TestTransferBuilder<T> txConstrained(
    T fromTrip,
    int fromStopIndex,
    T toTrip,
    int toStopIndex
  ) {
    var builder = tx(fromTrip, fromStopIndex, toTrip, toStopIndex);
    // Make sure the constraint is initialized; hence an object generated in the build step.
    // If none of the constraints are set this still generates a constraint instance, which
    // should behave like a regular transfer, but is not the same structure.
    builder.constraint();
    return builder;
  }

  public T fromTrip() {
    return fromTrip;
  }

  public TestTransferBuilder<T> fromTrip(T fromTrip) {
    this.fromTrip = fromTrip;
    return this;
  }

  public int fromStopIndex() {
    return fromStopIndex;
  }

  public TestTransferBuilder<T> fromStopIndex(int fromStopIndex) {
    this.fromStopIndex = fromStopIndex;
    return this;
  }

  public T toTrip() {
    return toTrip;
  }

  public TestTransferBuilder<T> toTrip(T toTrip) {
    this.toTrip = toTrip;
    return this;
  }

  public int toStopIndex() {
    return toStopIndex;
  }

  public TestTransferBuilder<T> toStopIndex(int toStopIndex) {
    this.toStopIndex = toStopIndex;
    return this;
  }

  /**
   * Walk duration in seconds
   */
  public int walk() {
    return walkDurationSec;
  }

  public TestTransferBuilder<T> walk(int walkDurationSec) {
    this.walkDurationSec = walkDurationSec;
    return this;
  }

  public TestTransferBuilder<T> staySeated() {
    this.constraint().staySeated();
    return this;
  }

  public TestTransferBuilder<T> guaranteed() {
    this.constraint().guaranteed();
    return this;
  }

  public TestTransferBuilder<T> maxWaitTime(int maxWaitTime) {
    this.constraint().maxWaitTime(maxWaitTime);
    return this;
  }

  public TestTransferBuilder<T> priority(TransferPriority priority) {
    this.constraint().priority(priority);
    return this;
  }

  public TripToTripTransfer<T> build() {
    validateFromTo();
    validateWalkDurationSec();

    var pathTransfer = fromStopIndex == toStopIndex
      ? null
      : TestTransfers.transfer(toStopIndex, walkDurationSec);

    return new TripToTripTransfer<>(
      departure(fromTrip, fromStopIndex),
      arrival(toTrip, toStopIndex),
      pathTransfer,
      buildConstrainedTransfer()
    );
  }

  private static <T extends RaptorTripSchedule> Trip createDummyTrip(T trip) {
    // Set an uniq id: pattern + the first stop departure time
    return TimetableRepositoryForTest
      .trip(trip.pattern().debugInfo() + ":" + TimeUtils.timeToStrCompact(trip.departure(0)))
      .build();
  }

  private ConstrainedTransfer buildConstrainedTransfer() {
    if (constraint == null) {
      return null;
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

  private static <T extends RaptorTripSchedule> TripStopTime<T> departure(T trip, int stopIndex) {
    return TripStopTime.departure(trip, trip.pattern().findStopPositionAfter(0, stopIndex));
  }

  private static <T extends RaptorTripSchedule> TripStopTime<T> arrival(T trip, int stopIndex) {
    return TripStopTime.arrival(trip, trip.pattern().findStopPositionAfter(0, stopIndex));
  }

  private void validateFromTo() {
    Objects.requireNonNull(fromTrip);
    Objects.requireNonNull(toTrip);
    if (fromStopIndex < 0) {
      throw new IllegalStateException();
    }
    if (toStopIndex < 0) {
      throw new IllegalStateException();
    }
  }

  private void validateWalkDurationSec() {
    if (walkDurationSec < 0) {
      throw new IllegalStateException();
    }
  }

  private TransferConstraint.Builder constraint() {
    if (constraint == null) {
      constraint = TransferConstraint.of();
    }
    return constraint;
  }
}
