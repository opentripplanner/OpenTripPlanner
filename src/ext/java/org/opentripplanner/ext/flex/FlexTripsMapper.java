package org.opentripplanner.ext.flex;

import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.onebusaway.gtfs.model.StopTime.MISSING_VALUE;

public class FlexTripsMapper {

  private static final Logger LOG = LoggerFactory.getLogger(FlexTripsMapper.class);

  static public void createFlexTrips(OtpTransitServiceBuilder builder) {
    TripStopTimes stopTimesByTrip = builder.getStopTimesSortedByTrip();

    final int tripSize = stopTimesByTrip.size();
    int tripCount = 0;

    for (org.opentripplanner.model.Trip trip : stopTimesByTrip.keys()) {
      if (++tripCount % 100000 == 0) {
        LOG.debug("Mapped StopTimes for flex trips {}/{}", tripCount, tripSize);
      }

      /* Fetch the stop times for this trip. Copy the list since it's immutable. */
      List<StopTime> stopTimes = new ArrayList<>(stopTimesByTrip.get(trip));

      if (isUnscheduledTrip(stopTimes)) {
        if (stopTimes.size() == 2) {
          // TODO: Drop this restriction after time handling and ride times are defined
          builder.getFlexTripsById().add(new UnscheduledTrip(trip, stopTimes));
        }
      } else if (isScheduledFlexTrip(stopTimes)) {
        builder.getFlexTripsById().add(new ScheduledDeviatedTrip(trip, stopTimes));
      } else if (hasContinuousStops(stopTimes)) {
        // builder.getFlexTripsById().add(new ContinuousPickupDropOffTrip(trip, stopTimes));
      }
    }
  }

  private static boolean isUnscheduledTrip(List<StopTime> stopTimes) {
    return stopTimes.stream().allMatch(st -> !st.isArrivalTimeSet() && !st.isDepartureTimeSet());
  }

  private static boolean isScheduledFlexTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> otherStopTypePredicate = Predicate.not(st -> st.getStop() instanceof Stop);
    Predicate<StopTime> noExplicitWindow = stopTime -> stopTime.getMaxDepartureTime() == MISSING_VALUE && stopTime.getMinArrivalTime() == MISSING_VALUE;
    return stopTimes.stream().anyMatch(otherStopTypePredicate) && stopTimes.stream().allMatch(noExplicitWindow);
  }

  private static boolean hasContinuousStops(List<StopTime> stopTimes) {
    return stopTimes
        .stream()
        .anyMatch(st -> st.getContinuousPickup() == 0 || st.getContinuousPickup() == 2
            || st.getContinuousPickup() == 3 || st.getContinuousDropOff() == 0
            || st.getContinuousDropOff() == 2 || st.getContinuousDropOff() == 3);
  }

}
