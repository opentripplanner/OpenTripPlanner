package org.opentripplanner.ext.flex.trip;

import java.util.Set;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.ext.flex.FlexParameters;

import static org.opentripplanner.model.PickDrop.NONE;

/**
 * This type of FlexTrip is used when a taxi-type service is modeled, which operates in one or
 * between two areas/groups of stops without a set schedule. The travel times are calculated based
 * on the driving time between the stops, with the schedule times being used just for deciding if a
 * trip is possible.
 */
public class UnscheduledTrip extends FlexTrip {
  // unscheduled trips can contain one or two stop_times
  private static final Set<Integer> N_STOPS = Set.of(1,2);

  private final UnscheduledStopTime[] stopTimes;

  private final BookingInfo[] dropOffBookingInfos;
  private final BookingInfo[] pickupBookingInfos;

  public static boolean isUnscheduledTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> noExplicitTimes = Predicate.not(st -> st.isArrivalTimeSet() || st.isDepartureTimeSet());
    Predicate<StopTime> notContinuousStop = stopTime ->
        stopTime.getFlexContinuousDropOff() == NONE.getGtfsCode() && stopTime.getFlexContinuousPickup() == NONE.getGtfsCode();
    return N_STOPS.contains(stopTimes.size())
        && stopTimes.stream().allMatch(noExplicitTimes)
        && stopTimes.stream().allMatch(notContinuousStop);
  }

  public UnscheduledTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    if (!isUnscheduledTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for unscheduled trip");
    }

    var size = stopTimes.size();
    this.stopTimes = new UnscheduledStopTime[size];
    this.dropOffBookingInfos = new BookingInfo[size];
    this.pickupBookingInfos = new BookingInfo[size];

    for (int i = 0; i < size; i++) {
      this.stopTimes[i] = new UnscheduledStopTime(stopTimes.get(i));
      this.dropOffBookingInfos[i] = stopTimes.get(0).getDropOffBookingInfo();
      this.pickupBookingInfos[i] = stopTimes.get(0).getPickupBookingInfo();
    }
  }

  @Override
  public Stream<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate date, FlexPathCalculator calculator, FlexParameters params
  ) {
    int fromIndex = getFromIndex(access);
    int toIndex = getToIndex(access);

    if (stopTimes[fromIndex].dropOffType == NONE.getGtfsCode()) { return Stream.empty(); }

    ArrayList<FlexAccessTemplate> res = new ArrayList<>();

    for (StopLocation stop : expandStops(stopTimes[fromIndex].stop)) {
      res.add(new FlexAccessTemplate(access, this, fromIndex, toIndex, stop, date, calculator, params));
    }

    return res.stream();
  }

  @Override
  public Stream<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate date, FlexPathCalculator calculator, FlexParameters params
  ) {
    int fromIndex = getFromIndex(egress);
    int toIndex = getToIndex(egress);

    if (stopTimes[toIndex].pickupType == NONE.getGtfsCode()) { return Stream.empty(); }

    ArrayList<FlexEgressTemplate> res = new ArrayList<>();

    for (StopLocation stop : expandStops(stopTimes[toIndex].stop)) {
      res.add(new FlexEgressTemplate(egress, this, fromIndex, toIndex, stop, date, calculator, params));
    }

    return res.stream();
  }

  @Override
  public int earliestDepartureTime(
      int departureTime, int fromStopIndex, int toStopIndex, int flexTime
  ) {
    UnscheduledStopTime fromStopTime = stopTimes[fromStopIndex];
    UnscheduledStopTime toStopTime = stopTimes[toStopIndex];
    if (fromStopTime.flexWindowEnd < departureTime || toStopTime.flexWindowEnd < (
        departureTime + flexTime
    )) {
      return -1;
    }

    return Math.max(departureTime, fromStopTime.flexWindowStart);
  }

  @Override
  public int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex, int flexTime) {
    UnscheduledStopTime fromStopTime = stopTimes[fromStopIndex];
    UnscheduledStopTime toStopTime = stopTimes[toStopIndex];
    if (toStopTime.flexWindowStart > arrivalTime || fromStopTime.flexWindowStart > (
        arrivalTime - flexTime
    )) {
      return -1;
    }

    return Math.min(arrivalTime, toStopTime.flexWindowEnd);
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
      if (stopTimes[i].pickupType == NONE.getGtfsCode()) continue;
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
      if (stopTimes[i].dropOffType == NONE.getGtfsCode()) continue;
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

  private static class UnscheduledStopTime implements Serializable {
    private final StopLocation stop;

    private final int flexWindowStart;
    private final int flexWindowEnd;

    private final int pickupType;
    private final int dropOffType;

    private UnscheduledStopTime(StopTime st) {
      stop = st.getStop();

      flexWindowStart = st.getFlexWindowStart();
      flexWindowEnd = st.getFlexWindowEnd();

      pickupType = st.getPickupType().getGtfsCode();
      dropOffType = st.getDropOffType().getGtfsCode();
    }
  }
}
