package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.ext.flex.distancecalculator.DistanceCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graphfinder.StopAtDistance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * This type of FlexTrip is used when a taxi-type service is modeled, which operates in one or
 * between two areas/groups of stops without a set schedule. The travel times are calculated based
 * on the driving time between the stops, with the schedule times being used just for deciding if a
 * trip is possible.
 */
public class UnscheduledTrip extends FlexTrip {
  private static final int N_STOPS = 2;

  private final UnscheduledStopTime[] stopTimes;

  public static boolean isUnscheduledTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> noExplicitTimes = Predicate.not(st -> st.isArrivalTimeSet() || st.isDepartureTimeSet());
    Predicate<StopTime> notContinuousStop = stopTime ->
        stopTime.getContinuousDropOff() == 1 && stopTime.getContinuousPickup() == 1;
    return stopTimes.size() == N_STOPS
        && stopTimes.stream().allMatch(noExplicitTimes)
        && stopTimes.stream().allMatch(notContinuousStop);
  }

  public UnscheduledTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    if (!isUnscheduledTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for unscheduled trip");
    }

    this.stopTimes = new UnscheduledStopTime[N_STOPS];

    for (int i = 0; i < N_STOPS; i++) {
      this.stopTimes[i] = new UnscheduledStopTime(stopTimes.get(i));
    }
  }

  @Override
  public Stream<FlexAccessTemplate> getFlexAccessTemplates(
      StopAtDistance access, int differenceFromStartOfTime, ServiceDate serviceDate, DistanceCalculator calculator
  ) {
    int fromIndex = getFromIndex(access);

    if (fromIndex == -1) return Stream.empty();

    ArrayList<FlexAccessTemplate> res = new ArrayList<>();

    for (int toIndex = fromIndex + 1; toIndex < stopTimes.length; toIndex++) {
      if (stopTimes[toIndex].dropOffType == 1) continue;
      for (StopLocation stop : expandStops(stopTimes[toIndex].stop)) {
        res.add(new FlexAccessTemplate(access, this, fromIndex, toIndex, stop, differenceFromStartOfTime, serviceDate, calculator));
      }
    }

    return res.stream();
  }

  @Override
  public Stream<FlexEgressTemplate> getFlexEgressTemplates(
      StopAtDistance egress, int differenceFromStartOfTime, ServiceDate serviceDate, DistanceCalculator calculator
  ) {
    int toIndex = getToIndex(egress);

    if (toIndex == -1) return Stream.empty();

    ArrayList<FlexEgressTemplate> res = new ArrayList<>();

    for (int fromIndex = toIndex - 1; fromIndex >= 0; fromIndex--) {
      if (stopTimes[fromIndex].pickupType == 1) continue;
      for (StopLocation stop : expandStops(stopTimes[fromIndex].stop)) {
        res.add(new FlexEgressTemplate(egress, this, fromIndex, toIndex, stop, differenceFromStartOfTime, serviceDate, calculator));
      }
    }

    return res.stream();
  }

  @Override
  public int earliestDepartureTime(
      int departureTime, int fromStopIndex, int toStopIndex, int flexTime
  ) {
    UnscheduledStopTime fromStopTime = stopTimes[fromStopIndex];
    UnscheduledStopTime toStopTime = stopTimes[toStopIndex];
    if (fromStopTime.maxDepartureTime < departureTime || toStopTime.maxArrivalTime < (
        departureTime + flexTime
    )) {
      return -1;
    }

    return Math.max(departureTime, fromStopTime.minDepartureTime);
  }

  @Override
  public int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex, int flexTime) {
    UnscheduledStopTime fromStopTime = stopTimes[fromStopIndex];
    UnscheduledStopTime toStopTime = stopTimes[toStopIndex];
    if (toStopTime.minArrivalTime > arrivalTime || fromStopTime.minDepartureTime > (
        arrivalTime - flexTime
    )) {
      return -1;
    }

    return Math.min(arrivalTime, toStopTime.maxArrivalTime);
  }

  @Override
  public Collection<StopLocation> getStops() {
    return Arrays
        .stream(stopTimes)
        .map(scheduledDeviatedStopTime -> scheduledDeviatedStopTime.stop)
        .collect(Collectors.toSet());
  }

  private Collection<StopLocation> expandStops(StopLocation stop) {
    return stop instanceof FlexLocationGroup
        ? ((FlexLocationGroup) stop).getLocations()
        : Collections.singleton(stop);
  }

  private int getFromIndex(StopAtDistance accessEgress) {
    for (int i = 0; i < stopTimes.length; i++) {
      if (stopTimes[i].pickupType == 1) continue; // No pickup or dropoff allowed here
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

  private int getToIndex(StopAtDistance accessEgress) {
    for (int i = stopTimes.length - 1; i >= 0; i--) {
      if (stopTimes[i].dropOffType == 1) continue; // No pickup or dropoff allowed here
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

    private final int minArrivalTime;
    private final int minDepartureTime;
    private final int maxArrivalTime;
    private final int maxDepartureTime;

    private final int pickupType;
    private final int dropOffType;

    private UnscheduledStopTime(StopTime st) {
      stop = st.getStop();

      minArrivalTime = st.getMinArrivalTime();
      minDepartureTime = st.getMinArrivalTime(); //TODO
      maxArrivalTime = st.getMaxDepartureTime(); //TODO
      maxDepartureTime = st.getMaxDepartureTime();

      pickupType = st.getPickupType();
      dropOffType = st.getDropOffType();
    }
  }
}
