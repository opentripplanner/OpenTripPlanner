package org.opentripplanner.ext.flex;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.routing.api.request.framework.TimePenalty;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlexTripsMapper {

  private static final Logger LOG = LoggerFactory.getLogger(FlexTripsMapper.class);

  public static List<FlexTrip<?, ?>> createFlexTrips(
    OtpTransitServiceBuilder builder,
    DataImportIssueStore store
  ) {
    List<FlexTrip<?, ?>> result = new ArrayList<>();
    TripStopTimes stopTimesByTrip = builder.getStopTimesSortedByTrip();

    final int tripSize = stopTimesByTrip.size();

    ProgressTracker progress = ProgressTracker.track("Create flex trips", 500, tripSize);

    for (Trip trip : stopTimesByTrip.keys()) {
      var stopTimes = stopTimesByTrip.get(trip);
      if (UnscheduledTrip.isUnscheduledTrip(stopTimes)) {
        var timePenalty = builder.getFlexTimePenalty().getOrDefault(trip, TimePenalty.NONE);
        result.add(
          UnscheduledTrip.of(trip.getId())
            .withTrip(trip)
            .withStopTimes(stopTimes)
            .withTimePenalty(timePenalty)
            .build()
        );
      } else if (ScheduledDeviatedTrip.isScheduledDeviatedFlexTrip(stopTimes)) {
        result.add(
          ScheduledDeviatedTrip.of(trip.getId()).withTrip(trip).withStopTimes(stopTimes).build()
        );
      } else if (stopTimes.stream().anyMatch(StopTime::combinesContinuousStoppingWithFlexWindow)) {
        store.add(
          "ContinuousFlexStopTime",
          "Trip %s contains a stop time which combines flex with continuous pick up/drop off. This is an invalid combination: https://github.com/MobilityData/gtfs-flex/issues/70",
          trip.getId()
        );
        // result.add(new ContinuousPickupDropOffTrip(trip, stopTimes));
      } else if (
        stopTimes.size() < 2 &&
        stopTimes.stream().anyMatch(st -> st.hasFlexWindow() || st.hasFlexibleStop())
      ) {
        store.add(
          "InvalidFlexTrip",
          "Trip %s defines only a single stop time, which is invalid: https://gtfs.org/documentation/schedule/examples/flex/",
          trip.getId()
        );
      }

      //Keep lambda! A method-ref would cause incorrect class and line number to be logged
      //noinspection Convert2MethodRef
      progress.step(m -> LOG.info(m));
    }
    LOG.info(progress.completeMessage());
    LOG.info("Done creating flex trips. Created a total of {} trips.", result.size());
    return result;
  }
}
