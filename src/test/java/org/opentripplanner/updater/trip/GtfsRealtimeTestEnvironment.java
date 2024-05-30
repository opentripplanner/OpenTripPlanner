package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import java.time.Duration;
import java.util.List;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateResult;

public class GtfsRealtimeTestEnvironment {

  private static final TimetableSnapshotSourceParameters PARAMETERS = new TimetableSnapshotSourceParameters(
    Duration.ZERO,
    false
  );
  public final TimetableSnapshotSource source;

  public final RealtimeTestData testData = new RealtimeTestData();

  public GtfsRealtimeTestEnvironment() {
    source = new TimetableSnapshotSource(PARAMETERS, testData.transitModel);
  }

  public UpdateResult applyTripUpdates(GtfsRealtime.TripUpdate update) {
    return applyTripUpdates(List.of(update));
  }

  public UpdateResult applyTripUpdates(List<GtfsRealtime.TripUpdate> updates) {
    return source.applyTripUpdates(
      null,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA,
      true,
      updates,
      testData.getFeedId()
    );
  }

  public TripPattern getPatternForTrip(Trip trip) {
    return testData.transitModel.getTransitModelIndex().getPatternForTrip().get(trip);
  }
}
