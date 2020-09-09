package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This type of FlexTrip is used when a taxi-type service is modeled, which operates in one or
 * between two areas/groups of stops without a set schedule. The travel times are calculated based
 * on the driving time between the stops, with the schedule times being used just for deciding if a
 * trip is possible.
 */
public class UnscheduledTrip extends FlexTrip {

  private final UnscheduledStopTime[] stopTimes;

  public static boolean isUnscheduledTrip(List<StopTime> stopTimes) {
    return stopTimes.stream().allMatch(st -> !st.isArrivalTimeSet() && !st.isDepartureTimeSet());
  }

  public UnscheduledTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    if (!isUnscheduledTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for unscheduled trip");
    }

    int nStops = stopTimes.size();
    this.stopTimes = new UnscheduledStopTime[nStops];

    for (int i = 0; i < nStops; i++) {
      this.stopTimes[i] = new UnscheduledStopTime(stopTimes.get(i));
    }
  }

  @Override
  public Collection<StopLocation> getStops() {
    return Arrays
        .stream(stopTimes)
        .map(scheduledDeviatedStopTime -> scheduledDeviatedStopTime.stop)
        .collect(Collectors.toSet());
  }

  private static class UnscheduledStopTime {

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
