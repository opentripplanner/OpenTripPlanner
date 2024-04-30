package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.opentripplanner.raptor.api.model.RaptorConstants.TIME_NOT_SET;

import javax.annotation.Nullable;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfo;

public class BookingRestrictionAccessEgress implements RoutingAccessEgress {

  private final RoutingAccessEgress delegate;

  /**
   * The requested time the passenger will book the trip. Normally, this is when the search is
   * performed plus a small grace period to allow the user to complete the booking.
   */
  private final int requestedBookingTime;

  private final RoutingBookingInfo bookingInfo;

  public BookingRestrictionAccessEgress(
    RoutingAccessEgress delegate,
    RoutingBookingInfo bookingInfo,
    int requestedBookingTime
  ) {
    this.delegate = delegate;
    this.requestedBookingTime = requestedBookingTime;
    this.bookingInfo = bookingInfo;
  }

  public static RoutingAccessEgress decorateAccessEgressBookingRestriction(
    RoutingAccessEgress accessEgress,
    int requestedBookingTime
  ) {
    var bookingInfo = accessEgress.routingBookingInfo();
    return bookingInfo.isPresent()
      ? new BookingRestrictionAccessEgress(accessEgress, bookingInfo.get(), requestedBookingTime)
      : accessEgress;
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
    int edt = delegate.earliestDepartureTime(requestedDepartureTime);
    if (edt == TIME_NOT_SET) {
      return TIME_NOT_SET;
    }
    if (bookingInfo.isThereEnoughTimeToBookForDeparture(edt, requestedBookingTime)) {
      return edt;
    }
    return TIME_NOT_SET;
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    var lat = delegate.latestArrivalTime(requestedArrivalTime);
    if (lat == TIME_NOT_SET) {
      return TIME_NOT_SET;
    }
    if (bookingInfo.isThereEnoughTimeToBookForArrival(lat, requestedBookingTime)) {
      return lat;
    }
    return TIME_NOT_SET;
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
    return new BookingRestrictionAccessEgress(
      delegate.withPenalty(penalty),
      bookingInfo,
      requestedBookingTime
    );
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
