package org.opentripplanner.ext.flex.trip;

import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.ScheduledFlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitBuilder;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

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
    if (!isScheduledFlexTrip(stopTimes)) {
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

  public static boolean isScheduledFlexTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> notStopType = Predicate.not(st -> st.getStop() instanceof RegularStop);
    Predicate<StopTime> notContinuousStop = stopTime ->
      stopTime.getFlexContinuousDropOff() == NONE && stopTime.getFlexContinuousPickup() == NONE;
    return (
      stopTimes.stream().anyMatch(notStopType) && stopTimes.stream().allMatch(notContinuousStop)
    );
  }

  @Override
  public Stream<FlexAccessTemplate> getFlexAccessTemplates(
    NearbyStop access,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    FlexConfig config
  ) {
    FlexPathCalculator scheduledCalculator = new ScheduledFlexPathCalculator(calculator, this);

    int fromIndex = getFromIndex(access);

    if (fromIndex == -1) {
      return Stream.empty();
    }

    ArrayList<FlexAccessTemplate> res = new ArrayList<>();

    for (int toIndex = fromIndex; toIndex < stopTimes.length; toIndex++) {
      if (getAlightRule(toIndex).isNotRoutable()) {
        continue;
      }
      for (StopLocation stop : expandStops(stopTimes[toIndex].stop)) {
        res.add(
          new FlexAccessTemplate(
            access,
            this,
            fromIndex,
            toIndex,
            stop,
            date,
            scheduledCalculator,
            config
          )
        );
      }
    }

    return res.stream();
  }

  @Override
  public Stream<FlexEgressTemplate> getFlexEgressTemplates(
    NearbyStop egress,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    FlexConfig config
  ) {
    FlexPathCalculator scheduledCalculator = new ScheduledFlexPathCalculator(calculator, this);

    int toIndex = getToIndex(egress);

    if (toIndex == -1) {
      return Stream.empty();
    }

    ArrayList<FlexEgressTemplate> res = new ArrayList<>();

    for (int fromIndex = toIndex; fromIndex >= 0; fromIndex--) {
      if (getBoardRule(fromIndex).isNotRoutable()) {
        continue;
      }
      for (StopLocation stop : expandStops(stopTimes[fromIndex].stop)) {
        res.add(
          new FlexEgressTemplate(
            egress,
            this,
            fromIndex,
            toIndex,
            stop,
            date,
            scheduledCalculator,
            config
          )
        );
      }
    }

    return res.stream();
  }

  @Override
  public int earliestDepartureTime(
    int departureTime,
    int fromStopIndex,
    int toStopIndex,
    int flexTripDurationSeconds
  ) {
    int stopTime = MISSING_VALUE;
    for (int i = fromStopIndex; stopTime == MISSING_VALUE && i >= 0; i--) {
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
    int fromStopIndex,
    int toStopIndex,
    int flexTripDurationSeconds
  ) {
    int stopTime = MISSING_VALUE;
    for (int i = toStopIndex; stopTime == MISSING_VALUE && i < stopTimes.length; i++) {
      stopTime = stopTimes[i].arrivalTime;
    }
    return stopTime <= arrivalTime ? stopTime : MISSING_VALUE;
  }

  @Override
  public int latestArrivalTime(int stopIndex) {
    return stopTimes[stopIndex].arrivalTime;
  }

  @Override
  public Set<StopLocation> getStops() {
    return Arrays
      .stream(stopTimes)
      .map(scheduledDeviatedStopTime -> scheduledDeviatedStopTime.stop)
      .collect(Collectors.toSet());
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
  public boolean isBoardingPossible(NearbyStop stop) {
    return getFromIndex(stop) != -1;
  }

  @Override
  public boolean isAlightingPossible(NearbyStop stop) {
    return getToIndex(stop) != -1;
  }

  @Override
  public boolean sameAs(@Nonnull ScheduledDeviatedTrip other) {
    return (
      super.sameAs(other) &&
      Arrays.equals(stopTimes, other.stopTimes) &&
      Arrays.equals(pickupBookingInfos, other.pickupBookingInfos) &&
      Arrays.equals(dropOffBookingInfos, other.dropOffBookingInfos)
    );
  }

  @Nonnull
  @Override
  public TransitBuilder<ScheduledDeviatedTrip, ScheduledDeviatedTripBuilder> copy() {
    return new ScheduledDeviatedTripBuilder(this);
  }

  private Collection<StopLocation> expandStops(StopLocation stop) {
    return stop instanceof GroupStop groupStop
      ? groupStop.getChildLocations()
      : Collections.singleton(stop);
  }

  private int getFromIndex(NearbyStop accessEgress) {
    for (int i = 0; i < stopTimes.length; i++) {
      if (getBoardRule(i).isNotRoutable()) {
        continue;
      }
      StopLocation stop = stopTimes[i].stop;
      if (stop instanceof GroupStop groupStop) {
        if (groupStop.getChildLocations().contains(accessEgress.stop)) {
          return i;
        }
      } else {
        if (stop.equals(accessEgress.stop)) {
          return i;
        }
      }
    }
    return -1;
  }

  private int getToIndex(NearbyStop accessEgress) {
    for (int i = stopTimes.length - 1; i >= 0; i--) {
      if (getAlightRule(i).isNotRoutable()) {
        continue;
      }
      StopLocation stop = stopTimes[i].stop;
      if (stop instanceof GroupStop groupStop) {
        if (groupStop.getChildLocations().contains(accessEgress.stop)) {
          return i;
        }
      } else {
        if (stop.equals(accessEgress.stop)) {
          return i;
        }
      }
    }
    return -1;
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
