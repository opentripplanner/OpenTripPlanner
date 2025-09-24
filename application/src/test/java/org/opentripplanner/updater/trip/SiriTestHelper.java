package org.opentripplanner.updater.trip;

import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import java.util.List;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.DefaultRealTimeUpdateContext;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;
import org.opentripplanner.updater.trip.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.siri.updater.EstimatedTimetableHandler;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;

public class SiriTestHelper {

  private final RealtimeTestEnvironment realtimeTestEnvironment;
  private final SiriRealTimeTripUpdateAdapter siriAdapter;

  SiriTestHelper(RealtimeTestEnvironment realtimeTestEnvironment) {
    this.realtimeTestEnvironment = realtimeTestEnvironment;
    this.siriAdapter = new SiriRealTimeTripUpdateAdapter(
      realtimeTestEnvironment.timetableRepository(),
      realtimeTestEnvironment.timetableSnapshotManager()
    );
  }

  public static SiriTestHelper of(RealtimeTestEnvironment realtimeTestEnvironment) {
    return new SiriTestHelper(realtimeTestEnvironment);
  }

  public SiriEtBuilder etBuilder() {
    return new SiriEtBuilder(realtimeTestEnvironment.getDateTimeHelper());
  }

  public UpdateResult applyEstimatedTimetableWithFuzzyMatcher(
    List<EstimatedTimetableDeliveryStructure> updates
  ) {
    return applyEstimatedTimetable(updates, true);
  }

  public UpdateResult applyEstimatedTimetable(List<EstimatedTimetableDeliveryStructure> updates) {
    return applyEstimatedTimetable(updates, false);
  }

  public RealtimeTestEnvironment realtimeTestEnvironment() {
    return realtimeTestEnvironment;
  }

  private UpdateResult applyEstimatedTimetable(
    List<EstimatedTimetableDeliveryStructure> updates,
    boolean fuzzyMatching
  ) {
    UpdateResult updateResult = getEstimatedTimetableHandler(fuzzyMatching).applyUpdate(
      updates,
      DIFFERENTIAL,
      new DefaultRealTimeUpdateContext(
        new Graph(),
        realtimeTestEnvironment.timetableRepository(),
        realtimeTestEnvironment.timetableSnapshotManager().getTimetableSnapshotBuffer()
      )
    );
    commitTimetableSnapshot();
    return updateResult;
  }

  private EstimatedTimetableHandler getEstimatedTimetableHandler(boolean fuzzyMatching) {
    return new EstimatedTimetableHandler(
      siriAdapter,
      fuzzyMatching,
      realtimeTestEnvironment.getFeedId()
    );
  }

  private void commitTimetableSnapshot() {
    realtimeTestEnvironment.timetableSnapshotManager().purgeAndCommit();
  }
}
