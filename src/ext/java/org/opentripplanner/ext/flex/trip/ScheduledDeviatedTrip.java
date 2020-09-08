package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A scheduled deviated trip is similar to a regular scheduled trip, except that is continues stop
 * locations, which are not stops, but other types, such as groups of stops or location areas.
 */
public class ScheduledDeviatedTrip extends FlexTrip {

  private final ScheduledDeviatedStopTime[] stopTimes;

  public static boolean isScheduledFlexTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> notStopType = Predicate.not(st -> st.getStop() instanceof Stop);
    Predicate<StopTime> noExplicitWindows = stopTime ->
        stopTime.getMaxDepartureTime() == org.onebusaway.gtfs.model.StopTime.MISSING_VALUE
            && stopTime.getMinArrivalTime() == org.onebusaway.gtfs.model.StopTime.MISSING_VALUE;
    return stopTimes.stream().anyMatch(notStopType)
        && stopTimes.stream().allMatch(noExplicitWindows);
  }

  public ScheduledDeviatedTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    if (!isScheduledFlexTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for scheduled flex trip");
    }

    int nStops = stopTimes.size();
    this.stopTimes = new ScheduledDeviatedStopTime[nStops];

    for (int i = 0; i < nStops; i++) {
      this.stopTimes[i] = new ScheduledDeviatedStopTime(stopTimes.get(i));
    }
  }

  @Override
  public Collection<StopLocation> getStops() {
    return Arrays
        .stream(stopTimes)
        .map(scheduledDeviatedStopTime -> scheduledDeviatedStopTime.stop)
        .collect(Collectors.toSet());
  }

  private static class ScheduledDeviatedStopTime {
    private final StopLocation stop;
    private final int departureTime;
    private final int arrivalTime;
    private final int pickupType;
    private final int dropOffType;

    private ScheduledDeviatedStopTime(StopTime st) {
      this.stop = st.getStop();
      this.arrivalTime = st.getArrivalTime();
      this.departureTime = st.getDepartureTime();
      this.pickupType = st.getPickupType();
      this.dropOffType = st.getDropOffType();
    }
  }
}
