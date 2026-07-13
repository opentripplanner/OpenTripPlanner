package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateErrorType.NOT_IMPLEMENTED_DIFFERENTIAL_DUPLICATED;
import static org.opentripplanner.updater.spi.UpdateErrorType.OUTSIDE_SERVICE_PERIOD;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND;

import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;

/// Handles GTFS-RT TripUpdates for trips with schedule relationship `DUPLICATED`.
/// Creates a copy of a scheduled trip shifted to a new start time (and service date).
class DuplicatedTripHandler {

  private final TransitEditorService transitEditorService;
  private final TimetableSnapshotManager snapshotManager;
  private final DeduplicatorService deduplicator;

  DuplicatedTripHandler(
    TransitEditorService transitEditorService,
    TimetableSnapshotManager snapshotManager,
    DeduplicatorService deduplicator
  ) {
    this.transitEditorService = transitEditorService;
    this.snapshotManager = snapshotManager;
    this.deduplicator = deduplicator;
  }

  UpdateSuccess handleDuplicated(TripUpdate tripUpdate, UpdateIncrementality updateIncrementality)
    throws UpdateException {
    // out of precaution we don't allow the combination of differential and DUPLICATED
    // it's not clear what the semantics of this would be and particular how cancellation of a
    // duplicated trip would work.
    // please get in touch with the dev team if you need this functionality.
    if (updateIncrementality == UpdateIncrementality.DIFFERENTIAL) {
      throw UpdateException.of(tripUpdate.tripId(), NOT_IMPLEMENTED_DIFFERENTIAL_DUPLICATED);
    }
    tripUpdate.validateDuplicated();

    var originalTrip = transitEditorService.getTrip(tripUpdate.tripId());
    if (originalTrip == null) {
      throw UpdateException.of(tripUpdate.tripId(), TRIP_NOT_FOUND);
    }

    var serviceId = transitEditorService.getOrCreateServiceIdForDate(tripUpdate.startDate());
    if (serviceId == null) {
      throw UpdateException.of(tripUpdate.tripId(), OUTSIDE_SERVICE_PERIOD);
    }

    // Look up the original trip's pattern and scheduled times

    var originalPattern = transitEditorService.findPattern(originalTrip);
    var originalScheduledTimes = (ScheduledTripTimes) originalPattern
      .getScheduledTimetable()
      .getTripTimes(tripUpdate.tripId());

    // Calculate how many seconds to shift all stop times
    int originalFirstDeparture = originalScheduledTimes.getScheduledDepartureTime(0);
    int newFirstDeparture = tripUpdate.startTime().get().toSecondOfDay();
    int offsetSeconds = newFirstDeparture - originalFirstDeparture;

    // Build the new trip entity (copy of original with a new ID)
    var newTripId = duplicatedTripId(tripUpdate);
    var newTrip = Trip.of(newTripId)
      .withRoute(originalTrip.getRoute())
      .withServiceId(serviceId)
      .build();

    // Shift all scheduled times and rebind to the new trip
    int serviceCode = transitEditorService.getTripCalendars().getServiceCode(serviceId);
    var newScheduledTimes = originalScheduledTimes
      .copyOf(deduplicator)
      .withTrip(newTrip)
      .withServiceCode(serviceCode)
      .plusTimeShift(offsetSeconds)
      .build();

    // Produce real-time trip times marked as an added trip
    var newTripTimes = newScheduledTimes
      .createRealTimeFromScheduledTimes()
      .withServiceCode(serviceCode)
      .withAdded()
      .withRealTimeUpdated()
      .build();

    var tripOnServiceDate = TripOnServiceDate.of(newTripId)
      .withTrip(newTrip)
      .withServiceDate(tripUpdate.startDate())
      .build();

    var update = RealTimeTripUpdate.of(originalPattern, newTripTimes, tripUpdate.startDate())
      .withTripCreation(true)
      .withAddedTripOnServiceDate(tripOnServiceDate)
      .build();
    return snapshotManager.updateBuffer(update);
  }

  /// The spec is silent about how these ids should be constructed, so we create a new ID
  /// ourselves.
  /// It is therefore not possible to send a spec-compliant vehicle position update for this
  /// trip. If this is a requirement, then we need to update the spec.
  private static FeedScopedId duplicatedTripId(TripUpdate tripUpdate) {
    var localDateTime = tripUpdate.startDate().atTime(tripUpdate.startTime().orElseThrow());
    return new FeedScopedId(
      tripUpdate.tripId().getFeedId(),
      tripUpdate.tripId().getId() + ":duplicated:" + localDateTime
    );
  }
}
