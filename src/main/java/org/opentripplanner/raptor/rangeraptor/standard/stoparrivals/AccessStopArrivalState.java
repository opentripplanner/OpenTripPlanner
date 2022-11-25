package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * This class is responsible for adding access functionality, which the {@link
 * DefaultStopArrivalState} ignore. It is injected into the state matrix when new accesses come into
 * play (right round for flex and right iteration for time-restricted access, yet to be
 * implemented). We do this to keep the default state simple and small. This way we use less memory.
 * We use a delegate pattern and not inheritance, because this allows to decorate an egress state as
 * well as the default state. There are relatively few access states, so the memory and performance
 * overhead is small.
 */
public class AccessStopArrivalState<T extends RaptorTripSchedule> implements StopArrivalState<T> {

  private final DefaultStopArrivalState<T> delegate;
  private RaptorAccessEgress accessArriveOnStreet;
  private RaptorAccessEgress accessArriveOnBoard;

  public AccessStopArrivalState(
    int time,
    RaptorAccessEgress accessPath,
    boolean isOverallBestTime,
    DefaultStopArrivalState<T> other
  ) {
    this.delegate = other;
    setAccessTime(time, accessPath, isOverallBestTime);
  }

  /* Implement StopArrivalState */

  @Override
  public int time() {
    return delegate.time();
  }

  @Override
  public int onBoardArrivalTime() {
    return delegate.onBoardArrivalTime();
  }

  @Override
  public boolean reachedOnBoard() {
    return delegate.reachedOnBoard();
  }

  @Override
  public boolean reachedOnStreet() {
    return arrivedByAccessOnStreet() || arrivedByTransfer();
  }

  @Override
  public boolean arrivedByAccessOnStreet() {
    return accessArriveOnStreet != null;
  }

  @Override
  public RaptorAccessEgress accessPathOnStreet() {
    return accessArriveOnStreet;
  }

  @Override
  public boolean arrivedByAccessOnBoard() {
    return accessArriveOnBoard != null;
  }

  @Override
  public RaptorAccessEgress accessPathOnBoard() {
    return accessArriveOnBoard;
  }

  @Override
  public final boolean arrivedByTransit() {
    return delegate.arrivedByTransit();
  }

  @Override
  public T trip() {
    return delegate.trip();
  }

  @Override
  public int boardTime() {
    return delegate.boardTime();
  }

  @Override
  public int boardStop() {
    return delegate.boardStop();
  }

  @Override
  public void arriveByTransit(int arrivalTime, int boardStop, int boardTime, T trip) {
    accessArriveOnBoard = null;
    delegate.arriveByTransit(arrivalTime, boardStop, boardTime, trip);
  }

  @Override
  public void setBestTimeTransit(int time) {
    delegate.setBestTimeTransit(time);
  }

  @Override
  public boolean arrivedByTransfer() {
    return delegate.arrivedByTransfer();
  }

  @Override
  public int transferFromStop() {
    return delegate.transferFromStop();
  }

  @Override
  public RaptorTransfer transferPath() {
    return delegate.transferPath();
  }

  @Override
  public void transferToStop(int fromStop, int arrivalTime, RaptorTransfer transferPath) {
    accessArriveOnStreet = null;
    delegate.transferToStop(fromStop, arrivalTime, transferPath);
  }

  @Override
  public String toString() {
    var builder = ToStringBuilder.of(AccessStopArrivalState.class);
    delegate.toStringAddBody(builder);

    if (arrivedByAccessOnBoard()) {
      builder.addDurationSec("onBoard", accessArriveOnBoard.durationInSeconds());
    }

    if (arrivedByAccessOnStreet()) {
      builder.addDurationSec("onStreet", accessArriveOnStreet.durationInSeconds());
    }

    return builder.toString();
  }

  /* package local methods */

  void setAccessTime(int time, RaptorAccessEgress access, boolean isOverallBestTime) {
    this.delegate.setAccessTime(time, isOverallBestTime, access.stopReachedOnBoard());

    if (access.stopReachedOnBoard()) {
      accessArriveOnBoard = access;
    } else {
      accessArriveOnStreet = access;
    }
  }
}
