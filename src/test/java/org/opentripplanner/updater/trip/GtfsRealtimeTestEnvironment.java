package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import java.time.Duration;
import java.util.List;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateResult;

public class GtfsRealtimeTestEnvironment extends AbstractRealtimeTestEnvironment {

  public final TimetableSnapshotSource source;

  public GtfsRealtimeTestEnvironment() {
    super();
    var parameters = new TimetableSnapshotSourceParameters(Duration.ZERO, false);
    source  = new TimetableSnapshotSource(parameters, transitModel);
  }

  public UpdateResult applyTripUpdates(List<GtfsRealtime.TripUpdate> updates) {
    return source.applyTripUpdates(
      null,
      BackwardsDelayPropagationType.REQUIRED,
      true,
      updates,
      getFeedId()
    );
  }
}
