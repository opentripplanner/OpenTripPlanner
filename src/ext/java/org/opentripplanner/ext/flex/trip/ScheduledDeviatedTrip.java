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
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.ScheduledFlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graphfinder.NearbyStop;

/**
 * A scheduled deviated trip is similar to a regular scheduled trip, except that is continues stop
 * locations, which are not stops, but other types, such as groups of stops or location areas.
 */
public class ScheduledDeviatedTrip extends FlexTrip {

  private final ScheduledDeviatedStopTime[] stopTimes;

  private final BookingInfo[] dropOffBookingInfos;
  private final BookingInfo[] pickupBookingInfos;

  public static boolean isScheduledFlexTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> notStopType = Predicate.not(st -> st.getStop() instanceof Stop);
    Predicate<StopTime> notContinuousStop = stopTime ->
        stopTime.getFlexContinuousDropOff() == NONE.getGtfsCode() && stopTime.getFlexContinuousPickup() == NONE.getGtfsCode();
    return stopTimes.stream().anyMatch(notStopType)
        && stopTimes.stream().allMatch(notContinuousStop);
  }

  public ScheduledDeviatedTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

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

  @Override
  public Stream<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate date, FlexPathCalculator calculator, FlexParameters params
  ) {
    FlexPathCalculator scheduledCalculator = new ScheduledFlexPathCalculator(calculator, this);

    int fromIndex = getFromIndex(access);

    if (fromIndex == -1) { return Stream.empty(); }

    ArrayList<FlexAccessTemplate> res = new ArrayList<>();

    for (int toIndex = fromIndex; toIndex < stopTimes.length; toIndex++) {
      if (stopTimes[toIndex].dropOffType == NONE.getGtfsCode()) continue;
      for (StopLocation stop : expandStops(stopTimes[toIndex].stop)) {
        res.add(new FlexAccessTemplate(
                access,
                this,
                fromIndex,
                toIndex,
                stop,
                date,
                scheduledCalculator,
                params
        ));
      }
    }

    return res.stream();
  }

  @Override
  public Stream<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate date, FlexPathCalculator calculator, FlexParameters params
  ) {
    FlexPathCalculator scheduledCalculator = new ScheduledFlexPathCalculator(calculator, this);

    int toIndex = getToIndex(egress);

    if (toIndex == -1) { return Stream.empty(); }

    ArrayList<FlexEgressTemplate> res = new ArrayList<>();

    for (int fromIndex = toIndex; fromIndex >= 0; fromIndex--) {
      if (stopTimes[fromIndex].pickupType == NONE.getGtfsCode()) continue;
      for (StopLocation stop : expandStops(stopTimes[fromIndex].stop)) {
        res.add(new FlexEgressTemplate(
                egress,
                this,
                fromIndex,
                toIndex,
                stop,
                date,
                scheduledCalculator,
                params
        ));
      }
    }

    return res.stream();
  }

  @Override
  public int earliestDepartureTime(
      int departureTime, int fromStopIndex, int toStopIndex, int flexTime
  ) {
    int stopTime = MISSING_VALUE;
    for (int i = fromStopIndex; stopTime == MISSING_VALUE && i >= 0; i--) {
      stopTime = stopTimes[i].departureTime;
    }
    return stopTime != MISSING_VALUE && stopTime >= departureTime ? stopTime : -1;
  }

  @Override
  public int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex, int flexTime) {
    int stopTime = MISSING_VALUE;
    for (int i = toStopIndex; stopTime == MISSING_VALUE && i < stopTimes.length; i++) {
      stopTime = stopTimes[i].arrivalTime;
    }
    return stopTime != MISSING_VALUE && stopTime <= arrivalTime ? stopTime : -1;
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
  public boolean isBoardingPossible(NearbyStop stop) {
    return getFromIndex(stop) != -1;
  }

  @Override
  public boolean isAlightingPossible(NearbyStop stop) {
    return getToIndex(stop) != -1;
  }

  private Collection<StopLocation> expandStops(StopLocation stop) {
    return stop instanceof FlexLocationGroup
        ? ((FlexLocationGroup) stop).getLocations()
        : Collections.singleton(stop);
  }

  private int getFromIndex(NearbyStop accessEgress) {
    for (int i = 0; i < stopTimes.length; i++) {
      if (stopTimes[i].pickupType == NONE.getGtfsCode()) continue; // No pickup allowed here
      StopLocation stop = stopTimes[i].stop;
      if (stop instanceof FlexLocationGroup) {
        if (((FlexLocationGroup) stop).getLocations().contains(accessEgress.stop)) {
          return i;
        }
      }
      else {
        if (stop.equals(accessEgress.stop)) {
          return i;
        }
      }
    }
    return -1;
  }

  private int getToIndex(NearbyStop accessEgress) {
    for (int i = stopTimes.length - 1; i >= 0; i--) {
      if (stopTimes[i].dropOffType == NONE.getGtfsCode()) continue; // No drop off allowed here
      StopLocation stop = stopTimes[i].stop;
      if (stop instanceof FlexLocationGroup) {
        if (((FlexLocationGroup) stop).getLocations().contains(accessEgress.stop)) {
          return i;
        }
      }
      else {
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
    private final int pickupType;
    private final int dropOffType;

    private ScheduledDeviatedStopTime(StopTime st) {
      this.stop = st.getStop();

      // Store the time the user is guaranteed to arrive at latest
      this.arrivalTime = st.getFlexWindowEnd() != MISSING_VALUE ? st.getFlexWindowEnd() : st.getArrivalTime();
      // Store the time the user needs to be ready for pickup
      this.departureTime = st.getFlexWindowStart() != MISSING_VALUE ? st.getFlexWindowStart() : st.getDepartureTime();

      // TODO: Store the window for a stop, and allow the user to have an "unguaranteed"
      // pickup/dropoff between the start and end of the window

      this.pickupType = st.getPickupType().getGtfsCode();
      this.dropOffType = st.getDropOffType().getGtfsCode();
    }
  }
}
