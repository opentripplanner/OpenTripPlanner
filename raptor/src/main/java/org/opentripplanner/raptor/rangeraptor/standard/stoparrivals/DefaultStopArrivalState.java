package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.utils.time.TimeUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The main purpose of this class is to hold data for a given arrival at a stop and raptor round. It
 * should be as light-weight as possible to minimize memory consumption and cheap to create and
 * garbage collect.
 * <p/>
 * This class holds both the best transit and the best transfer to a stop if they exist for a given
 * round and stop. The normal case is that this class represents either a transit arrival or a
 * transfer arrival. We only keep both if the transfer is better, arriving before the transit.
 * <p/>
 * The reason we need to keep both the best transfer and the best transit for a given stop and round
 * is that we may arrive at a stop by transit, then in the same or later round we may arrive by
 * transit. If the transfer arrival is better than the transit arrival, it might be tempting to
 * remove the transit arrival, but this transit might be the best way (or only way) to get to
 * another stop by transfer.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
sealed class DefaultStopArrivalState<T extends RaptorTripSchedule>
  implements StopArrivalState<T>
  permits EgressStopArrivalState {

  /**
   * Used to initialize all none-time-based attributes.
   */
  static final int NOT_SET = -1;

  // Best time - access, transit or transfer
  private int bestArrivalTime = NOT_SET;

  // Best on board time - access or transit
  private int onBoardArrivalTime = NOT_SET;

  // Transit
  private T trip = null;
  private int boardStopPosition = NOT_SET;

  // Transfer
  private int transferFromStop = NOT_SET;
  private RaptorTransfer transferPath = null;

  DefaultStopArrivalState() {}

  @Override
  public final int time() {
    return bestArrivalTime;
  }

  @Override
  public final int onBoardArrivalTime() {
    return onBoardArrivalTime;
  }

  @Override
  public final boolean reachedOnBoard() {
    return onBoardArrivalTime != NOT_SET;
  }

  /* Access */

  @Override
  public final boolean arrivedByAccessOnStreet() {
    return false;
  }

  @Override
  public final RaptorAccessEgress accessPathOnStreet() {
    throw new IllegalStateException("This class do no handle access, see AccessStopArrivalState");
  }

  @Override
  public final boolean arrivedByAccessOnBoard() {
    return false;
  }

  @Override
  public final RaptorAccessEgress accessPathOnBoard() {
    throw new IllegalStateException("This class do no handle access, see AccessStopArrivalState");
  }

  /* Transit */

  @Override
  public boolean arrivedByTransit() {
    return boardStopPosition != NOT_SET;
  }

  @Override
  public final T trip() {
    return trip;
  }

  @Override
  public final int boardTime() {
    return trip.departure(boardStopPosition);
  }

  @Override
  public final int boardStopPosition() {
    return boardStopPosition;
  }

  @Override
  public void arriveByTransit(int arrivalTime, int boardStopPosition, T trip) {
    this.onBoardArrivalTime = arrivalTime;
    this.boardStopPosition = boardStopPosition;
    this.trip = trip;
  }

  @Override
  public final void setBestTimeTransit(int time) {
    this.bestArrivalTime = time;
    // The transfer is cleared since it is not the fastest alternative any more.
    this.transferFromStop = NOT_SET;
  }

  /* Transfer */

  @Override
  public final boolean arrivedByTransfer() {
    return transferFromStop != NOT_SET;
  }

  @Override
  public final int transferFromStop() {
    return transferFromStop;
  }

  @Override
  public final RaptorTransfer transferPath() {
    return transferPath;
  }

  @Override
  public void transferToStop(int fromStop, int arrivalTime, RaptorTransfer transferPath) {
    this.bestArrivalTime = arrivalTime;
    this.transferFromStop = fromStop;
    this.transferPath = transferPath;
  }

  /* other methods */

  @Override
  public String toString() {
    return toStringAddBody(ToStringBuilder.of(DefaultStopArrivalState.class)).toString();
  }

  /** This allows subclasses to attach content and type to their own toString() */
  ToStringBuilder toStringAddBody(ToStringBuilder builder) {
    builder
      .addServiceTime("arrivalTime", bestArrivalTime, NOT_SET)
      .addServiceTime("onBoardArrivalTime", onBoardArrivalTime, NOT_SET)
      .addNum("boardStopPosition", boardStopPosition, NOT_SET)
      .addObj("trip", tripInfo())
      .addNum("transferFromStop", transferFromStop, NOT_SET);

    if (transferPath != null) {
      builder.addDurationSec("transfer", transferPath.durationInSeconds());
    }
    return builder;
  }

  void setAccessTime(int time, boolean isBestTimeOverall, boolean onBoard) {
    if (isBestTimeOverall) {
      this.bestArrivalTime = time;
      // Clear transfer to avoid mistakes
      this.transferFromStop = NOT_SET;
      this.transferPath = null;
    }
    if (onBoard) {
      this.onBoardArrivalTime = time;
      // Clear transit to avoid mistakes
      this.trip = null;
      this.boardStopPosition = NOT_SET;
    }
  }

  private String tripInfo() {
    return boardStopPosition == NOT_SET
      ? null
      : trip.pattern().debugInfo() +
        " @" +
        TimeUtils.timeToStrCompact(trip.departure(boardStopPosition));
  }
}
