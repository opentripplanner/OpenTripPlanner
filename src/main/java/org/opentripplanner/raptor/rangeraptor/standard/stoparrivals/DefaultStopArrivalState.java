package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * The main purpose of this class is to hold data for a given arrival at a stop and raptor round. It
 * should be as light-weight as possible to minimize memory consumption and cheap to create and
 * garbage collect.
 * <p/>
 * This class holds both the best transit and the best transfer to a stop if they exist for a given
 * round and stop. The normal case is that this class represent either a transit arrival or a
 * transfer arrival. We only keep both if the transfer is better, arriving before the transit.
 * <p/>
 * The reason we need to keep both the best transfer and the best transit for a given stop and round
 * is that we may arrive at a stop by transit, then in the same or later round we may arrive by
 * transit. If the transfer arrival is better then the transit arrival it might be tempting to
 * remove the transit arrival, but this transit might be the best way (or only way) to get to
 * another stop by transfer.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
class DefaultStopArrivalState<T extends RaptorTripSchedule> implements StopArrivalState<T> {

  /**
   * Used to initialize all none time based attributes.
   */
  static final int NOT_SET = -1;

  // Best time - access, transit or transfer
  private int bestArrivalTime = NOT_SET;

  // Best on board time - access or transit
  private int onBoardArrivalTime = NOT_SET;

  // Transit
  private T trip = null;
  private int boardTime = NOT_SET;
  private int boardStop = NOT_SET;

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

  @Override
  public final boolean reachedOnStreet() {
    return arrivedByTransfer();
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
    return boardStop != NOT_SET;
  }

  @Override
  public final T trip() {
    return trip;
  }

  @Override
  public final int boardTime() {
    return boardTime;
  }

  @Override
  public final int boardStop() {
    return boardStop;
  }

  @Override
  public void arriveByTransit(int arrivalTime, int boardStop, int boardTime, T trip) {
    this.onBoardArrivalTime = arrivalTime;
    this.trip = trip;
    this.boardTime = boardTime;
    this.boardStop = boardStop;
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
      .addNum("boardStop", boardStop, NOT_SET)
      .addServiceTime("boardTime", boardTime, NOT_SET)
      .addObj("trip", trip == null ? null : trip.pattern().debugInfo())
      .addNum("transferFromStop", transferFromStop, NOT_SET);

    if (transferPath != null) {
      builder.addDurationSec("transfer", transferPath.durationInSeconds());
    }
    return builder;
  }

  void setAccessTime(int time, boolean isBestTimeOverall, boolean onBoard) {
    if (isBestTimeOverall) {
      this.bestArrivalTime = time;
      // Clear transit to avoid mistakes
      this.transferFromStop = NOT_SET;
      this.transferPath = null;
    }
    if (onBoard) {
      this.onBoardArrivalTime = time;
      // Clear transit to avoid mistakes
      this.trip = null;
      this.boardTime = NOT_SET;
      this.boardStop = NOT_SET;
    }
  }
}
