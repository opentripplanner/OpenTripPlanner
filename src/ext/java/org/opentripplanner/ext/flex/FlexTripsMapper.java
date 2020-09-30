package org.opentripplanner.ext.flex;

import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

public class FlexTripsMapper {

  private static final Logger LOG = LoggerFactory.getLogger(FlexTripsMapper.class);

  static public void createFlexTrips(OtpTransitServiceBuilder builder) {
    TripStopTimes stopTimesByTrip = builder.getStopTimesSortedByTrip();

    final int tripSize = stopTimesByTrip.size();

    ProgressTracker progress = ProgressTracker.track(
        "Create flex trips", 500, tripSize
    );

    for (org.opentripplanner.model.Trip trip : stopTimesByTrip.keys()) {

      /* Fetch the stop times for this trip. Copy the list since it's immutable. */
      List<StopTime> stopTimes = new ArrayList<>(stopTimesByTrip.get(trip));

      if (UnscheduledTrip.isUnscheduledTrip(stopTimes)) {
        builder.getFlexTripsById().add(new UnscheduledTrip(trip, stopTimes));
      } else if (ScheduledDeviatedTrip.isScheduledFlexTrip(stopTimes)) {
        builder.getFlexTripsById().add(new ScheduledDeviatedTrip(trip, stopTimes));
      } else if (hasContinuousStops(stopTimes)) {
        // builder.getFlexTripsById().add(new ContinuousPickupDropOffTrip(trip, stopTimes));
      }

      //Keep lambda! A method-ref would causes incorrect class and line number to be logged
      progress.step(m -> LOG.info(m));
    }
    LOG.info(progress.completeMessage());
    LOG.info("Done creating flex trips. Created a total of {} trips.", builder.getFlexTripsById().size());
  }

  private static boolean hasContinuousStops(List<StopTime> stopTimes) {
    return stopTimes
        .stream()
        .anyMatch(st -> st.getFlexContinuousPickup() != PICKDROP_NONE || st.getFlexContinuousDropOff() != PICKDROP_NONE);
  }

}
