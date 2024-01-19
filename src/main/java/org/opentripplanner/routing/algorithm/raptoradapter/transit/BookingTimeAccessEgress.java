package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.Instant;
import java.time.ZoneId;
import javax.annotation.Nullable;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.booking.RoutingBookingInfo;
import org.opentripplanner.street.search.state.State;

public class BookingTimeAccessEgress implements RoutingAccessEgress {

  private final RoutingAccessEgress delegate;

  private final OpeningHoursAdjuster openingHoursAdjuster;

  public BookingTimeAccessEgress(
    RoutingBookingInfo bookingInfo,
    Instant requestDateTime,
    Instant earliestBookingTime,
    ZoneId timeZone,
    RoutingAccessEgress delegate
  ) {
    this.delegate = delegate;
    openingHoursAdjuster =
      new OpeningHoursAdjuster(
        bookingInfo,
        delegate,
        earliestBookingTime,
        requestDateTime,
        timeZone
      );
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
  public int earliestDepartureTime(int requestedDepartureTime) {
    return openingHoursAdjuster.earliestDepartureTime(requestedDepartureTime);
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    return delegate.latestArrivalTime(requestedArrivalTime);
  }

  @Override
  public boolean hasOpeningHours() {
    return delegate.hasOpeningHours();
  }

  @Override
  @Nullable
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
  public RoutingAccessEgress withPenalty(TimeAndCost penalty) {
    return delegate.withPenalty(penalty);
  }

  @Override
  public State getLastState() {
    return delegate.getLastState();
  }

  @Override
  public boolean isWalkOnly() {
    return delegate.isWalkOnly();
  }

  @Override
  public boolean hasPenalty() {
    return delegate.hasPenalty();
  }

  @Override
  public TimeAndCost penalty() {
    return delegate.penalty();
  }
}
