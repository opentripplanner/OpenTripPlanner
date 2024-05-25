package org.opentripplanner.ext.siri;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.AbstractRealtimeTestEnvironment;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

public class SiriRealtimeTestEnvironment extends AbstractRealtimeTestEnvironment {

  private final SiriTimetableSnapshotSource source;

  public SiriRealtimeTestEnvironment() {
    super();
    var parameters = new TimetableSnapshotSourceParameters(Duration.ZERO, false);
    source  = new SiriTimetableSnapshotSource(parameters, transitModel);
  }

  public UpdateResult applyEstimatedTimetable(List<EstimatedTimetableDeliveryStructure> updates) {
    return source.applyEstimatedTimetable(
        null,
        getEntityResolver(),
        getFeedId(),
        false,
        updates
      );
  }

}
