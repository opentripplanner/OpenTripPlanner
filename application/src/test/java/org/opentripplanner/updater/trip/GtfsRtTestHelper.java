package org.opentripplanner.updater.trip;

import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.transit.realtime.GtfsRealtime;
import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model.framework.DeduplicatorService;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.gtfs.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.ForwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.GtfsRealTimeTripUpdateAdapter;

public class GtfsRtTestHelper {

  private final TransitTestEnvironment transitTestEnvironment;
  private final GtfsRealTimeTripUpdateAdapter gtfsAdapter;

  GtfsRtTestHelper(TransitTestEnvironment transitTestEnvironment) {
    this.transitTestEnvironment = transitTestEnvironment;
    this.gtfsAdapter = new GtfsRealTimeTripUpdateAdapter(
      transitTestEnvironment.timetableRepository(),
      DeduplicatorService.NOOP,
      transitTestEnvironment.timetableSnapshotManager(),
      transitTestEnvironment::defaultServiceDate
    );
  }

  public static GtfsRtTestHelper of(TransitTestEnvironment transitTestEnvironment) {
    return new GtfsRtTestHelper(transitTestEnvironment);
  }

  public TripUpdateBuilder tripUpdateScheduled(String tripId) {
    return tripUpdate(tripId, GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED);
  }

  public TripUpdateBuilder tripUpdateScheduled(String tripId, LocalDate serviceDate) {
    return tripUpdate(
      tripId,
      serviceDate,
      GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED
    );
  }

  public TripUpdateBuilder tripUpdate(
    String tripId,
    GtfsRealtime.TripDescriptor.ScheduleRelationship scheduleRelationship
  ) {
    return tripUpdate(tripId, transitTestEnvironment.defaultServiceDate(), scheduleRelationship);
  }

  public TripUpdateBuilder tripUpdate(
    String tripId,
    LocalDate serviceDate,
    GtfsRealtime.TripDescriptor.ScheduleRelationship scheduleRelationship
  ) {
    return new TripUpdateBuilder(
      tripId,
      serviceDate,
      scheduleRelationship,
      transitTestEnvironment.timeZone()
    );
  }

  public UpdateResult applyTripUpdate(GtfsRealtime.TripUpdate update) {
    return applyTripUpdates(List.of(update), FULL_DATASET);
  }

  public UpdateResult applyTripUpdate(
    GtfsRealtime.TripUpdate update,
    UpdateIncrementality incrementality
  ) {
    return applyTripUpdates(List.of(update), incrementality);
  }

  public UpdateResult applyTripUpdates(List<GtfsRealtime.TripUpdate> updates) {
    return applyTripUpdates(updates, FULL_DATASET);
  }

  public UpdateResult applyTripUpdates(
    List<GtfsRealtime.TripUpdate> updates,
    UpdateIncrementality incrementality
  ) {
    UpdateResult updateResult = gtfsAdapter.applyTripUpdates(
      null,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA,
      incrementality,
      updates,
      transitTestEnvironment.feedId()
    );
    commitTimetableSnapshot();
    return updateResult;
  }

  private void commitTimetableSnapshot() {
    transitTestEnvironment.timetableSnapshotManager().purgeAndCommit();
  }
}
