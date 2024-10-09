package org.opentripplanner.raptor.api.model;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Using delegation to extend the {@link RaptorAccessEgress} functionality is common, so we provide
 * a base delegation implementation here. This implementation delegates all operations to the
 * delegate.
 */
public class AbstractAccessEgressDecorator implements RaptorAccessEgress {

  private final RaptorAccessEgress delegate;

  public AbstractAccessEgressDecorator(RaptorAccessEgress delegate) {
    this.delegate = delegate;
  }

  protected RaptorAccessEgress delegate() {
    return delegate;
  }

  @Override
  public int stop() {
    return delegate.stop();
  }

  @Override
  public int c1() {
    return delegate.c1();
  }

  @Override
  public int durationInSeconds() {
    return delegate.durationInSeconds();
  }

  @Override
  public int timePenalty() {
    return delegate.timePenalty();
  }

  @Override
  public boolean hasTimePenalty() {
    return delegate.hasTimePenalty();
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    return delegate.earliestDepartureTime(requestedDepartureTime);
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    return delegate.latestArrivalTime(requestedArrivalTime);
  }

  @Override
  public boolean hasOpeningHours() {
    return delegate.hasOpeningHours();
  }

  @Nullable
  @Override
  public String openingHoursToString() {
    return delegate.openingHoursToString();
  }

  @Override
  public int numberOfRides() {
    return delegate.numberOfRides();
  }

  @Override
  public boolean hasRides() {
    return delegate.hasRides();
  }

  @Override
  public boolean stopReachedOnBoard() {
    return delegate.stopReachedOnBoard();
  }

  @Override
  public boolean stopReachedByWalking() {
    return delegate.stopReachedByWalking();
  }

  @Override
  public boolean isFree() {
    return delegate.isFree();
  }

  @Override
  public String defaultToString() {
    return delegate.defaultToString();
  }

  @Override
  public String asString(boolean includeStop, boolean includeCost, @Nullable String summary) {
    return delegate.asString(includeStop, includeCost, summary);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractAccessEgressDecorator that = (AbstractAccessEgressDecorator) o;
    return Objects.equals(delegate, that.delegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }
}
