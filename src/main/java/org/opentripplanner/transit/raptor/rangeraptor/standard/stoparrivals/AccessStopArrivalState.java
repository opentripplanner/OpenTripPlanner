package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * This class is responsible for adding access functionality, witch the
 * {@link DefaultStopArrivalState} ignore. It is injected into the state matrix when new accesses
 * come into play (right round for flex and right iteration for time-restricted access, jet to be
 * implemented). We do this to keep the default state simple and small. This way we use less memory.
 * We use a delegate pattern and not inheritance, because this allows to decorate an egress state
 * as well as the default state. There are relatively few access states, so the memory and
 * performance overhead is small.
 */
class AccessStopArrivalState<T extends RaptorTripSchedule> implements StopArrivalState<T> {

  private final DefaultStopArrivalState<T> delegate;

  AccessStopArrivalState(int time, RaptorTransfer accessPath, DefaultStopArrivalState<T> other) {
    this.delegate = other;
    setAccessTime(time, accessPath);
  }

  @Override
  public final boolean arrivedByAccess() {
    return true;
  }

  @Override
  public int time() {
    return delegate.time();
  }

  @Override
  public int onBoardArrivalTime() {
    return delegate.onBoardArrivalTime();
  }

  @Override
  public boolean reached() {
    return delegate.reached();
  }

  @Override
  public RaptorTransfer accessPath() {
    return delegate.accessPath();
  }

  @Override
  public boolean arrivedByTransit() {
    return false;
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
  public void transferToStop(
          int fromStop, int arrivalTime, RaptorTransfer transferPath
  ) {
    delegate.transferToStop(fromStop, arrivalTime, transferPath);
  }

  @Override
  public String toString() {
    var builder = ToStringBuilder.of(AccessStopArrivalState.class);
    delegate.toStringAddBody(builder);
    return builder.toString();
  }

  /* package local methods */

  void setAccessTime(int time, RaptorTransfer access) {
    this.delegate.setAccessTime(time, access);
  }
}
