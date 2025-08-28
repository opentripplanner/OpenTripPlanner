package org.opentripplanner.ext.flex.trip;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.TimePenaltyCalculator;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.api.request.framework.TimePenalty;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitBuilder;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.lang.IntRange;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * This type of FlexTrip is used when a taxi-type service is modeled, which operates in any number
 * of areas/groups of stops without a set schedule. The travel times are calculated based
 * on the driving time between the stops, with the schedule times being used just for deciding if a
 * trip is possible.
 * <p>
 * An unscheduled flex trip may visit/drive from one flex stops(areas/group of stop locations) to
 * any other stop in the pattern without driving through the stops in between. Only the times in the
 * two stops used need to match the path.
 * <p>
 * For a discussion of this behaviour see https://github.com/MobilityData/gtfs-flex/issues/76
 */
public class UnscheduledTrip extends FlexTrip<UnscheduledTrip, UnscheduledTripBuilder> {

  private final StopTimeWindow[] stopTimes;

  private final BookingInfo[] dropOffBookingInfos;
  private final BookingInfo[] pickupBookingInfos;

  private final TimePenalty timePenalty;

  public UnscheduledTrip(UnscheduledTripBuilder builder) {
    super(builder);
    List<StopTime> stopTimes = builder.stopTimes();
    if (!isUnscheduledTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for unscheduled trip");
    }

    var size = stopTimes.size();
    this.stopTimes = new StopTimeWindow[size];
    this.dropOffBookingInfos = new BookingInfo[size];
    this.pickupBookingInfos = new BookingInfo[size];

    for (int i = 0; i < size; i++) {
      this.stopTimes[i] = new StopTimeWindow(stopTimes.get(i));
      this.dropOffBookingInfos[i] = stopTimes.get(i).getDropOffBookingInfo();
      this.pickupBookingInfos[i] = stopTimes.get(i).getPickupBookingInfo();
    }
    this.timePenalty = Objects.requireNonNull(builder.timePenalty());
    DurationUtils.requireNonNegative(timePenalty.constant());
    DoubleUtils.requireInRange(timePenalty.coefficient(), 0.05d, Double.MAX_VALUE);
  }

  public static UnscheduledTripBuilder of(FeedScopedId id) {
    return new UnscheduledTripBuilder(id);
  }

  /**
   * Tests if the stop times constitute an {@link UnscheduledTrip}.
   * <p>
   * Returns true for the following cases:
   *  - A single fixed scheduled stop followed by a flexible one
   *  - One or more stop times with a flexible time window but no fixed stop in between them
   */
  public static boolean isUnscheduledTrip(List<StopTime> stopTimes) {
    if (stopTimes.size() < 2) {
      return false;
    } else if (stopTimes.stream().anyMatch(StopTime::combinesContinuousStoppingWithFlexWindow)) {
      return false;
      // special case: one fixed stop and a flexible window
    } else if (stopTimes.size() == 2) {
      return stopTimes.stream().anyMatch(StopTime::hasFlexWindow);
    } else {
      return stopTimes.stream().allMatch(StopTime::hasFlexWindow);
    }
  }

  @Override
  public int earliestDepartureTime(
    int requestedDepartureTime,
    int boardStopPosition,
    int alightStopPosition,
    int tripDurationSeconds
  ) {
    var optionalDepartureTimeWindow = departureTimeWindow(
      boardStopPosition,
      alightStopPosition,
      tripDurationSeconds
    );

    if (optionalDepartureTimeWindow.isEmpty()) {
      return MISSING_VALUE;
    }
    var win = optionalDepartureTimeWindow.get();
    if (win.endInclusive() < requestedDepartureTime) {
      return MISSING_VALUE;
    }
    return Math.max(requestedDepartureTime, win.startInclusive());
  }

  @Override
  public int earliestDepartureTime(int stopIndex) {
    return stopTimes[stopIndex].start();
  }

  @Override
  public int latestArrivalTime(
    int requestedArrivalTime,
    int boardStopPosition,
    int alightStopPosition,
    int tripDurationSeconds
  ) {
    var optionalArrivalTimeWindow = arrivalTimeWindow(
      boardStopPosition,
      alightStopPosition,
      tripDurationSeconds
    );

    if (optionalArrivalTimeWindow.isEmpty()) {
      return MISSING_VALUE;
    }
    var win = optionalArrivalTimeWindow.get();
    if (win.startInclusive() > requestedArrivalTime) {
      return MISSING_VALUE;
    }
    return Math.min(requestedArrivalTime, win.endInclusive());
  }

  @Override
  public int latestArrivalTime(int stopIndex) {
    return stopTimes[stopIndex].end();
  }

  @Override
  public int numberOfStops() {
    return stopTimes.length;
  }

  @Override
  public Set<StopLocation> getStops() {
    return Arrays.stream(stopTimes).map(StopTimeWindow::stop).collect(Collectors.toSet());
  }

  @Override
  public StopLocation getStop(int stopIndex) {
    return stopTimes[stopIndex].stop();
  }

  @Override
  public BookingInfo getDropOffBookingInfo(int stopIndex) {
    return dropOffBookingInfos[stopIndex];
  }

  @Override
  public BookingInfo getPickupBookingInfo(int stopIndex) {
    return pickupBookingInfos[stopIndex];
  }

  @Override
  public PickDrop getBoardRule(int stopIndex) {
    return stopTimes[stopIndex].pickupType();
  }

  @Override
  public PickDrop getAlightRule(int stopIndex) {
    return stopTimes[stopIndex].dropOffType();
  }

  @Override
  public boolean isBoardingPossible(StopLocation stop) {
    return findBoardIndex(stop) != STOP_INDEX_NOT_FOUND;
  }

  @Override
  public boolean isAlightingPossible(StopLocation stop) {
    return findAlightIndex(stop) != STOP_INDEX_NOT_FOUND;
  }

  @Override
  public boolean sameAs(UnscheduledTrip other) {
    return (
      super.sameAs(other) &&
      Arrays.equals(stopTimes, other.stopTimes) &&
      Arrays.equals(pickupBookingInfos, other.pickupBookingInfos) &&
      Arrays.equals(dropOffBookingInfos, other.dropOffBookingInfos)
    );
  }

  @Override
  public TransitBuilder<UnscheduledTrip, UnscheduledTripBuilder> copy() {
    return new UnscheduledTripBuilder(this);
  }

  @Override
  public int findBoardIndex(StopLocation fromStop) {
    for (int i = 0; i < stopTimes.length; i++) {
      if (getBoardRule(i).isNotRoutable()) {
        continue;
      }
      StopLocation stop = stopTimes[i].stop();
      if (stop instanceof GroupStop groupStop) {
        if (groupStop.getChildLocations().contains(fromStop)) {
          return i;
        }
      } else {
        if (stop.equals(fromStop)) {
          return i;
        }
      }
    }
    return FlexTrip.STOP_INDEX_NOT_FOUND;
  }

  @Override
  public int findAlightIndex(StopLocation toStop) {
    for (int i = stopTimes.length - 1; i >= 0; i--) {
      if (getAlightRule(i).isNotRoutable()) {
        continue;
      }
      StopLocation stop = stopTimes[i].stop();
      if (stop instanceof GroupStop groupStop) {
        if (groupStop.getChildLocations().contains(toStop)) {
          return i;
        }
      } else {
        if (stop.equals(toStop)) {
          return i;
        }
      }
    }
    return FlexTrip.STOP_INDEX_NOT_FOUND;
  }

  @Override
  public FlexPathCalculator decorateFlexPathCalculator(FlexPathCalculator defaultCalculator) {
    // Get the correct {@link FlexPathCalculator} depending on the {@code timePenalty}.
    // If the modifier does not change the result, we return the regular calculator.
    if (timePenalty.modifies()) {
      return new TimePenaltyCalculator(defaultCalculator, timePenalty);
    } else {
      return defaultCalculator;
    }
  }

  private Optional<IntRange> departureTimeWindow(
    int boardStopPosition,
    int alightStopPosition,
    int tripDurationSeconds
  ) {
    // Align the from and to time-windows by subtracting the trip-duration from the to-time-window.
    var fromTime = stopTimes[boardStopPosition].timeWindow();
    var toTimeShifted = stopTimes[alightStopPosition].timeWindow().minus(tripDurationSeconds);

    // Then take the intersection of the aligned windows to find the window where the
    // requested-departure-time must be within
    return fromTime.intersect(toTimeShifted);
  }

  private Optional<IntRange> arrivalTimeWindow(
    int boardStopPosition,
    int alightStopPosition,
    int tripDurationSeconds
  ) {
    // Align the from and to time-windows by adding the trip-duration to the from-time-window.
    var fromTimeShifted = stopTimes[boardStopPosition].timeWindow().plus(tripDurationSeconds);
    var toTime = stopTimes[alightStopPosition].timeWindow();

    // Then take the intersection of the aligned windows to find the window where the
    // requested-arrival-time must be within
    return toTime.intersect(fromTimeShifted);
  }

  private record IndexedStopLocation(int index, StopLocation stop) {}
}
