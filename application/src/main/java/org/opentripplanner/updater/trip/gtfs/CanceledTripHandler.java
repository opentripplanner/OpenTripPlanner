package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateErrorType.NO_TRIP_FOR_CANCELLATION_FOUND;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TripUpdateApplier;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;

/**
 * Handles GTFS-RT TripUpdates for trips with schedule relationship {@code CANCELED} or
 * {@code DELETED}. For differential feeds, first checks whether a previously added trip should be
 * canceled; otherwise cancels the matching scheduled trip in the timetable snapshot buffer.
 */
class CanceledTripHandler {

  private final TransitEditorService transitEditorService;
  private final TimetableRepository buffer;

  CanceledTripHandler(TransitEditorService transitEditorService, TimetableRepository buffer) {
    this.transitEditorService = transitEditorService;
    this.buffer = buffer;
  }

  UpdateSuccess cancel(TripUpdate tripUpdate, UpdateIncrementality incrementality)
    throws UpdateException {
    return handle(tripUpdate, CancelationType.CANCEL, incrementality);
  }

  UpdateSuccess delete(TripUpdate tripUpdate, UpdateIncrementality incrementality)
    throws UpdateException {
    return handle(tripUpdate, CancelationType.DELETE, incrementality);
  }

  private UpdateSuccess handle(
    TripUpdate tripUpdate,
    CancelationType cancelationType,
    UpdateIncrementality incrementality
  ) throws UpdateException {
    // For DIFFERENTIAL updates, try to cancel a previously added trip
    if (incrementality != FULL_DATASET) {
      var addedPattern = buffer.getNewTripPatternForModifiedTrip(
        tripUpdate.tripId(),
        tripUpdate.startDate()
      );
      if (addedPattern != null) {
        var timetable = buffer.resolve(addedPattern, tripUpdate.startDate());
        if (timetable != null) {
          var tripTimes = timetable.getTripTimes(tripUpdate.tripId());
          if (tripTimes != null && tripTimes.isAdded()) {
            var builder = tripTimes.createRealTimeFromScheduledTimes();
            switch (cancelationType) {
              case CANCEL -> builder.withCanceled();
              case DELETE -> builder.withDeleted();
            }
            return TripUpdateApplier.apply(
              buffer,
              RealTimeTripUpdate.of(addedPattern, builder.build(), tripUpdate.startDate()).build()
            );
          }
        }
      }
    }

    // Cancel the scheduled trip
    var pattern = getPatternForTripId(tripUpdate.tripId());
    if (pattern == null) {
      throw UpdateException.of(tripUpdate.tripId(), NO_TRIP_FOR_CANCELLATION_FOUND);
    }

    var tripTimes = pattern.getScheduledTimetable().getTripTimes(tripUpdate.tripId());
    if (tripTimes == null) {
      throw UpdateException.of(tripUpdate.tripId(), NO_TRIP_FOR_CANCELLATION_FOUND);
    }

    var builder = tripTimes.createRealTimeFromScheduledTimes();
    switch (cancelationType) {
      case CANCEL -> builder.withCanceled();
      case DELETE -> builder.withDeleted();
    }
    return TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(pattern, builder.build(), tripUpdate.startDate())
        .withRevertPreviousRealTimeUpdates(true)
        .build()
    );
  }

  private TripPattern getPatternForTripId(FeedScopedId tripId) {
    Trip trip = transitEditorService.getTrip(tripId);
    return transitEditorService.findPattern(trip);
  }

  private enum CancelationType {
    CANCEL,
    DELETE,
  }
}
