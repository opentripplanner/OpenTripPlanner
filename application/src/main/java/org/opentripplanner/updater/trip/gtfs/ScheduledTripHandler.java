package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_UPDATES;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
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

  private final TransitService transitService;
  private final TimetableRepository buffer;
  private final TripTimesUpdater tripTimesUpdater;
  private final TripPatternCache tripPatternCache;

  ScheduledTripHandler(
    TransitService transitService,
    TimetableRepository buffer,
    TripTimesUpdater tripTimesUpdater,
    TripPatternCache tripPatternCache
  ) {
    this.transitService = transitService;
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

    var serviceId = transitService.getTrip(tripUpdate.tripId()).getServiceId();
    var serviceDates = transitService.getTripCalendars().listServiceDates(serviceId);
    if (!serviceDates.contains(tripUpdate.startDate())) {
      throw UpdateException.of(tripUpdate.tripId(), NO_SERVICE_ON_DATE);
    }

    var tripTimesPatch = tripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      pattern.getScheduledTimetable(),
      tripUpdate,
      forwardsDelayPropagationType,
      backwardsDelayPropagationType
    );

    var updatedPickup = tripTimesPatch.updatedPickup();
    var updatedDropoff = tripTimesPatch.updatedDropoff();
    var replacedStopIndices = tripTimesPatch.replacedStopIndices();
    var updatedTripTimes = tripTimesPatch.tripTimes();

    Map<Integer, StopLocation> newStops = new HashMap<>();
    for (var entry : replacedStopIndices.entrySet()) {
      var stop = transitService.getRegularStop(
        new FeedScopedId(tripUpdate.tripId().getFeedId(), entry.getValue())
      );
      if (stop != null) {
        newStops.put(entry.getKey(), stop);
      }
    }

    // If there are stops with different pickup / drop off, or replaced stops, we need to change the pattern from the scheduled one
    if (!updatedPickup.isEmpty() || !updatedDropoff.isEmpty() || !newStops.isEmpty()) {
      StopPattern newStopPattern = pattern
        .copyPlannedStopPattern()
        .updatePickups(updatedPickup)
        .updateDropoffs(updatedDropoff)
        .replaceStops(newStops)
        .build();

      final Trip trip = transitService.getTrip(tripUpdate.tripId());
      final TripPattern newPattern = tripPatternCache.getOrCreateTripPattern(
        newStopPattern,
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

  private TripPattern getPatternForTripId(FeedScopedId tripId) {
    Trip trip = transitService.getTrip(tripId);
    return transitService.findPattern(trip);
  }
}
