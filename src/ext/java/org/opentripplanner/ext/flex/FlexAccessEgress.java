package org.opentripplanner.ext.flex;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfo;

public final class FlexAccessEgress {

  private final RegularStop stop;
  private final FlexPathDurations pathDurations;
  private final int fromStopIndex;
  private final int toStopIndex;
  private final FlexTrip<?, ?> trip;
  private final State lastState;
  private final boolean stopReachedOnBoard;

  @Nullable
  private final RoutingBookingInfo routingBookingInfo;

  public FlexAccessEgress(
    RegularStop stop,
    FlexPathDurations pathDurations,
    int fromStopIndex,
    int toStopIndex,
    FlexTrip<?, ?> trip,
    State lastState,
    boolean stopReachedOnBoard
  ) {
    this.stop = stop;
    this.pathDurations = pathDurations;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.trip = Objects.requireNonNull(trip);
    this.lastState = lastState;
    this.stopReachedOnBoard = stopReachedOnBoard;
    this.routingBookingInfo = createRoutingBookingInfo().orElse(null);
  }

  public RegularStop stop() {
    return stop;
  }

  public State lastState() {
    return lastState;
  }

  public boolean stopReachedOnBoard() {
    return stopReachedOnBoard;
  }

  public int earliestDepartureTime(int departureTime) {
    int requestedDepartureTime = pathDurations.mapToFlexTripDepartureTime(departureTime);
    int earliestDepartureTime = trip.earliestDepartureTime(
      requestedDepartureTime,
      fromStopIndex,
      toStopIndex,
      pathDurations.trip()
    );
    if (earliestDepartureTime == MISSING_VALUE) {
      return MISSING_VALUE;
    }
    return pathDurations.mapToRouterDepartureTime(earliestDepartureTime);
  }

  public int latestArrivalTime(int arrivalTime) {
    int requestedArrivalTime = pathDurations.mapToFlexTripArrivalTime(arrivalTime);
    int latestArrivalTime = trip.latestArrivalTime(
      requestedArrivalTime,
      fromStopIndex,
      toStopIndex,
      pathDurations.trip()
    );
    if (latestArrivalTime == MISSING_VALUE) {
      return MISSING_VALUE;
    }
    return pathDurations.mapToRouterArrivalTime(latestArrivalTime);
  }

  /**
   * Return routing booking info for the boarding stop. Empty, if there are not any
   * booking restrictions, witch applies to routing.
   */
  public Optional<RoutingBookingInfo> routingBookingInfo() {
    return Optional.ofNullable(routingBookingInfo);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(FlexAccessEgress.class)
      .addNum("fromStopIndex", fromStopIndex)
      .addNum("toStopIndex", toStopIndex)
      .addObj("durations", pathDurations)
      .addObj("stop", stop)
      .addObj("trip", trip.getId())
      .addObj("lastState", lastState)
      .addBoolIfTrue("stopReachedOnBoard", stopReachedOnBoard)
      .toString();
  }

  private Optional<RoutingBookingInfo> createRoutingBookingInfo() {
    var bookingInfo = trip.getPickupBookingInfo(fromStopIndex);
    if (bookingInfo == null) {
      return Optional.empty();
    }
    return bookingInfo.createRoutingBookingInfo(pathDurations.access());
  }
}
