package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.street.search.state.State;

public class BookingTimeAccessEgress implements RoutingAccessEgress {

  private static final int DAY_IN_SECONDS = 3600 * 24;

  private final RoutingAccessEgress delegate;

  private final BookingInfo pickupBookingInfo;
  private final int earliestBookingTime;

  public BookingTimeAccessEgress(
    RoutingAccessEgress delegate,
    Instant dateTime,
    Instant earliestBookingTime,
    ZoneId timeZone
  ) {
    this.delegate = delegate;
    this.earliestBookingTime = calculateOtpTime(dateTime, earliestBookingTime, timeZone);
    if (delegate instanceof FlexAccessEgressAdapter flexAccessEgressAdapter) {
      pickupBookingInfo = flexAccessEgressAdapter.getFlexTrip().getPickupBookingInfo(0);
    } else {
      pickupBookingInfo = null;
    }
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
    if (edt == RaptorConstants.TIME_NOT_SET) {
      return edt;
    }
    if (pickupBookingInfo != null) {
      return pickupBookingInfo.earliestDepartureTime(edt, earliestBookingTime);
    }
    return edt;
  }

  private int calculateOtpTime(Instant dateTime, Instant earliestBookingTime, ZoneId timeZone) {
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(dateTime, timeZone);
    ZonedDateTime zonedEarliestBookingTime = ZonedDateTime.ofInstant(earliestBookingTime, timeZone);
    int days = zonedDateTime.toLocalDate().until(zonedEarliestBookingTime.toLocalDate()).getDays();
    return zonedEarliestBookingTime.toLocalTime().toSecondOfDay() + days * DAY_IN_SECONDS;
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

  @Override
  public int timeShiftDepartureTimeToActualTime(int computedDepartureTimeIncludingPenalty) {
    return delegate.timeShiftDepartureTimeToActualTime(computedDepartureTimeIncludingPenalty);
  }
}
