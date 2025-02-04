package org.opentripplanner.ext.flex.trip;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.ScheduledFlexPathCalculator;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitBuilder;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

/**
 * A scheduled deviated trip is similar to a regular scheduled trip, except that it contains stop
 * locations, which are not stops, but other types, such as groups of stops or location areas.
 */
public class ScheduledDeviatedTrip
  extends FlexTrip<ScheduledDeviatedTrip, ScheduledDeviatedTripBuilder> {

  private final ScheduledDeviatedStopTime[] stopTimes;

  private final BookingInfo[] dropOffBookingInfos;
  private final BookingInfo[] pickupBookingInfos;

  ScheduledDeviatedTrip(ScheduledDeviatedTripBuilder builder) {
    super(builder);
    List<StopTime> stopTimes = builder.stopTimes();
    if (!isScheduledDeviatedFlexTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for scheduled flex trip");
    }

    int nStops = stopTimes.size();
    this.stopTimes = new ScheduledDeviatedStopTime[nStops];
    this.dropOffBookingInfos = new BookingInfo[nStops];
    this.pickupBookingInfos = new BookingInfo[nStops];

    for (int i = 0; i < nStops; i++) {
      this.stopTimes[i] = new ScheduledDeviatedStopTime(stopTimes.get(i));
      this.dropOffBookingInfos[i] = stopTimes.get(i).getDropOffBookingInfo();
      this.pickupBookingInfos[i] = stopTimes.get(i).getPickupBookingInfo();
    }
  }

  public static ScheduledDeviatedTripBuilder of(FeedScopedId id) {
    return new ScheduledDeviatedTripBuilder(id);
  }

  public static boolean isScheduledDeviatedFlexTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> notFixedStop = Predicate.not(st -> st.getStop() instanceof RegularStop);
    return (
      stopTimes.stream().anyMatch(notFixedStop) &&
      stopTimes.stream().noneMatch(StopTime::combinesContinuousStoppingWithFlexWindow)
    );
  }

  @Override
  public int earliestDepartureTime(
    int departureTime,
    int boardStopPosition,
    int alightStopPosition,
    int flexTripDurationSeconds
  ) {
    int stopTime = MISSING_VALUE;
    for (int i = boardStopPosition; stopTime == MISSING_VALUE && i >= 0; i--) {
      stopTime = stopTimes[i].departureTime;
    }
    return stopTime >= departureTime ? stopTime : MISSING_VALUE;
  }

  @Override
  public int earliestDepartureTime(int stopIndex) {
    return stopTimes[stopIndex].departureTime;
  }

  @Override
  public int latestArrivalTime(
    int arrivalTime,
    int boardStopPosition,
    int alightStopPosition,
    int flexTripDurationSeconds
  ) {
    int stopTime = MISSING_VALUE;
    for (int i = alightStopPosition; stopTime == MISSING_VALUE && i < stopTimes.length; i++) {
      stopTime = stopTimes[i].arrivalTime;
    }
    return stopTime <= arrivalTime ? stopTime : MISSING_VALUE;
  }

  @Override
  public int latestArrivalTime(int stopIndex) {
    return stopTimes[stopIndex].arrivalTime;
  }

  @Override
  public int numberOfStops() {
    return stopTimes.length;
  }

  @Override
  public Set<StopLocation> getStops() {
    return Arrays
      .stream(stopTimes)
      .map(scheduledDeviatedStopTime -> scheduledDeviatedStopTime.stop)
      .collect(Collectors.toSet());
  }

  @Override
  public StopLocation getStop(int stopIndex) {
    return stopTimes[stopIndex].stop;
  }

  @Override
  public BookingInfo getDropOffBookingInfo(int i) {
    return dropOffBookingInfos[i];
  }

  @Override
  public BookingInfo getPickupBookingInfo(int i) {
    return pickupBookingInfos[i];
  }

  @Override
  public PickDrop getBoardRule(int i) {
    return stopTimes[i].pickupType;
  }

  @Override
  public PickDrop getAlightRule(int i) {
    return stopTimes[i].dropOffType;
  }

  @Override
  public boolean isBoardingPossible(StopLocation fromStop) {
    return findBoardIndex(fromStop) != STOP_INDEX_NOT_FOUND;
  }

  @Override
  public boolean isAlightingPossible(StopLocation toStop) {
    return findAlightIndex(toStop) != STOP_INDEX_NOT_FOUND;
  }

  @Override
  public boolean sameAs(ScheduledDeviatedTrip other) {
    return (
      super.sameAs(other) &&
      Arrays.equals(stopTimes, other.stopTimes) &&
      Arrays.equals(pickupBookingInfos, other.pickupBookingInfos) &&
      Arrays.equals(dropOffBookingInfos, other.dropOffBookingInfos)
    );
  }

  @Override
  public TransitBuilder<ScheduledDeviatedTrip, ScheduledDeviatedTripBuilder> copy() {
    return new ScheduledDeviatedTripBuilder(this);
  }

  @Override
  public int findBoardIndex(StopLocation fromStop) {
    for (int i = 0; i < stopTimes.length; i++) {
      if (getBoardRule(i).isNotRoutable()) {
        continue;
      }
      StopLocation stop = stopTimes[i].stop;
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
    return STOP_INDEX_NOT_FOUND;
  }

  @Override
  public int findAlightIndex(StopLocation toStop) {
    for (int i = stopTimes.length - 1; i >= 0; i--) {
      if (getAlightRule(i).isNotRoutable()) {
        continue;
      }
      StopLocation stop = stopTimes[i].stop;
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
    return STOP_INDEX_NOT_FOUND;
  }

  @Override
  public FlexPathCalculator decorateFlexPathCalculator(FlexPathCalculator defaultCalculator) {
    return new ScheduledFlexPathCalculator(defaultCalculator, this);
  }

  private static class ScheduledDeviatedStopTime implements Serializable {

    private final StopLocation stop;
    private final int departureTime;
    private final int arrivalTime;
    private final PickDrop pickupType;
    private final PickDrop dropOffType;

    private ScheduledDeviatedStopTime(StopTime st) {
      this.stop = st.getStop();

      // Store the time the user is guaranteed to arrive at latest
      this.arrivalTime = st.getLatestPossibleArrivalTime();
      // Store the time the user needs to be ready for pickup
      this.departureTime = st.getEarliestPossibleDepartureTime();

      // TODO: Store the window for a stop, and allow the user to have an "unguaranteed"
      // pickup/dropoff between the start and end of the window

      // Do not allow for pickup/dropoff if times are not available. We do not support interpolation
      // for flex trips currently
      this.pickupType = departureTime == MISSING_VALUE ? PickDrop.NONE : st.getPickupType();
      this.dropOffType = arrivalTime == MISSING_VALUE ? PickDrop.NONE : st.getDropOffType();
    }
  }
}
