package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_UPDATES;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.repository.MutableTimetableSnapshot;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TripUpdateApplier;
import org.opentripplanner.updater.trip.gtfs.interpolation.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.interpolation.ForwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;

/**
 * Handles GTFS-RT TripUpdates for trips with schedule relationship {@code SCHEDULED}.
 * Applies real-time delay and stop-change patches on top of the static
 * timetable.
 */
class ScheduledTripHandler {

  private final TransitEditorService transitEditorService;
  private final MutableTimetableSnapshot buffer;
  private final TripTimesUpdater tripTimesUpdater;
  private final TripPatternCache tripPatternCache;

  ScheduledTripHandler(
    TransitEditorService transitEditorService,
    MutableTimetableSnapshot buffer,
    TripTimesUpdater tripTimesUpdater,
    TripPatternCache tripPatternCache
  ) {
    this.transitEditorService = transitEditorService;
    this.buffer = buffer;
    this.tripTimesUpdater = tripTimesUpdater;
    this.tripPatternCache = tripPatternCache;
  }

  UpdateSuccess handle(
    TripUpdate tripUpdate,
    ForwardsDelayPropagationType forwardsDelayPropagationType,
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) throws UpdateException {
    final TripPattern pattern = getPatternForTripId(tripUpdate.tripId());

    if (pattern == null) {
      throw UpdateException.of(tripUpdate.tripId(), TRIP_NOT_FOUND);
    }

    if (tripUpdate.stopTimeUpdates().isEmpty()) {
      throw UpdateException.of(tripUpdate.tripId(), NO_UPDATES);
    }

    var serviceId = transitEditorService.getTrip(tripUpdate.tripId()).getServiceId();
    var serviceDates = transitEditorService.getTripCalendars().listServiceDates(serviceId);
    if (!serviceDates.contains(tripUpdate.startDate())) {
      throw UpdateException.of(tripUpdate.tripId(), NO_SERVICE_ON_DATE);
    }

    var tripTimesPatch = tripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      pattern.getScheduledTimetable(),
      tripUpdate,
      forwardsDelayPropagationType,
      backwardsDelayPropagationType
    );

    // A pickup / drop-off override or a resolvable stop replacement moves the trip onto a pattern
    // differing from the scheduled one. The trip is then reported as MODIFIED.
    var feedId = tripUpdate.tripId().getFeedId();
    var modifiedStopPattern = tripTimesPatch
      .stopPatternChanges()
      .deriveStopPattern(pattern, stopId -> resolveStop(feedId, stopId));
    var patternModified = modifiedStopPattern.isPresent();
    if (patternModified) {
      tripTimesPatch.withModifiedTripPattern();
    }

    // Materialize and validate the trip times before touching the pattern cache, so that an
    // invalid update does not leave an orphan real-time pattern behind.
    var updatedTripTimes = tripTimesPatch.tripTimes();

    if (patternModified) {
      final Trip trip = transitEditorService.getTrip(tripUpdate.tripId());
      final TripPattern newPattern = tripPatternCache.getOrCreateTripPattern(
        modifiedStopPattern.get(),
        trip,
        pattern
      );

      return TripUpdateApplier.apply(
        buffer,
        RealTimeTripUpdate.of(newPattern, updatedTripTimes, tripUpdate.startDate())
          .withRevertPreviousRealTimeUpdates(true)
          .withHideTripInScheduledPattern(pattern)
          .build()
      );
    } else {
      return TripUpdateApplier.apply(
        buffer,
        RealTimeTripUpdate.of(pattern, updatedTripTimes, tripUpdate.startDate())
          .withRevertPreviousRealTimeUpdates(true)
          .build()
      );
    }
  }

  private StopLocation resolveStop(String feedId, String stopId) {
    return transitEditorService.getRegularStop(new FeedScopedId(feedId, stopId));
  }

  private TripPattern getPatternForTripId(FeedScopedId tripId) {
    Trip trip = transitEditorService.getTrip(tripId);
    return transitEditorService.findPattern(trip);
  }
}
