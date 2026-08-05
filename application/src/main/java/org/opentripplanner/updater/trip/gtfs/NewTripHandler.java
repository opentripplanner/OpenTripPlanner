package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.spi.UpdateErrorType.OUTSIDE_SERVICE_PERIOD;
import static org.opentripplanner.updater.spi.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_ALREADY_EXISTS;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TripUpdateApplier;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;

/**
 * Handles GTFS-RT TripUpdates for trips with schedule relationship {@code NEW}, {@code ADDED}, or
 * {@code REPLACEMENT}. Builds a new {@link org.opentripplanner.transit.model.timetable.Trip} (and
 * route, if needed) from the feed message and maps its stop-time updates to known stops.
 */
class NewTripHandler {

  private final TransitEditorService transitEditorService;
  private final TimetableRepository buffer;
  private final TripTimesUpdater tripTimesUpdater;
  private final TripPatternCache tripPatternCache;

  NewTripHandler(
    TransitEditorService transitEditorService,
    TimetableRepository buffer,
    TripTimesUpdater tripTimesUpdater,
    TripPatternCache tripPatternCache
  ) {
    this.transitEditorService = transitEditorService;
    this.buffer = buffer;
    this.tripTimesUpdater = tripTimesUpdater;
    this.tripPatternCache = tripPatternCache;
  }

  /**
   * Validate and handle GTFS-RT TripUpdate message containing a NEW trip.
   */
  UpdateSuccess handleNew(final TripUpdate tripUpdate) throws UpdateException {
    if (transitEditorService.getScheduledTrip(tripUpdate.tripId()) != null) {
      throw UpdateException.of(tripUpdate.tripId(), TRIP_ALREADY_EXISTS);
    }
    var serviceId = transitEditorService.getOrCreateServiceIdForDate(tripUpdate.startDate());
    if (serviceId == null) {
      throw UpdateException.of(tripUpdate.tripId(), OUTSIDE_SERVICE_PERIOD);
    }

    var result = new RouteFactory(transitEditorService).getOrCreate(tripUpdate);

    // TODO: which Agency ID to use? Currently use feed id.
    var tripBuilder = Trip.of(tripUpdate.tripId())
      .withRoute(result.route())
      .withServiceId(serviceId);

    tripUpdate.tripHeadsign().ifPresent(tripBuilder::withHeadsign);
    tripUpdate.tripShortName().ifPresent(tripBuilder::withShortName);

    Trip trip = tripBuilder.build();

    return handleNewOrReplacementTrip(trip, tripUpdate, true, false, result.newRouteCreated());
  }

  /**
   * Validate and handle GTFS-RT TripUpdate message containing a REPLACEMENT trip.
   */
  UpdateSuccess handleReplacement(TripUpdate tripUpdate) throws UpdateException {
    Trip trip = transitEditorService.getTrip(tripUpdate.tripId());

    if (trip == null) {
      throw UpdateException.of(tripUpdate.tripId(), TRIP_NOT_FOUND);
    }

    final Set<FeedScopedId> serviceIds = transitEditorService
      .getTripCalendars()
      .listServiceIdsOnServiceDate(tripUpdate.startDate());
    if (!serviceIds.contains(trip.getServiceId())) {
      // TODO: should we support this and change service id of trip?
      throw UpdateException.of(tripUpdate.tripId(), NO_SERVICE_ON_DATE);
    }

    return handleNewOrReplacementTrip(trip, tripUpdate, false, true, false);
  }

  /**
   * Handle GTFS-RT TripUpdate message containing a NEW or REPLACEMENT trip.
   */
  private UpdateSuccess handleNewOrReplacementTrip(
    Trip trip,
    TripUpdate tripUpdate,
    boolean added,
    boolean modified,
    boolean hasANewRouteBeenCreated
  ) throws UpdateException {
    FeedScopedId tripId = trip.getId();
    var stopAndStopTimeUpdates = matchStopsToStopTimeUpdates(tripUpdate);

    var warnings = new ArrayList<UpdateSuccess.WarningType>(0);

    if (stopAndStopTimeUpdates.size() < tripUpdate.stopTimeUpdates().size()) {
      warnings.add(UpdateSuccess.WarningType.UNKNOWN_STOPS_REMOVED_FROM_ADDED_TRIP);
    }

    if (stopAndStopTimeUpdates.size() < 2) {
      throw UpdateException.of(tripId, TOO_FEW_STOPS);
    }

    var value = tripTimesUpdater.createNewTripTimesFromGtfsRt(
      trip,
      tripUpdate,
      stopAndStopTimeUpdates,
      added,
      modified,
      transitEditorService.getTripCalendars().getServiceCode(trip.getServiceId())
    );

    return addNewOrReplacementTripToSnapshot(
      value,
      tripUpdate.startDate(),
      added,
      modified,
      hasANewRouteBeenCreated
    ).addWarnings(warnings);
  }

  /**
   * Add a new or replacement trip to the snapshot.
   */
  private UpdateSuccess addNewOrReplacementTripToSnapshot(
    final TripTimesWithStopPattern tripTimesWithStopPattern,
    final LocalDate serviceDate,
    final boolean added,
    final boolean modified,
    final boolean hasANewRouteBeenCreated
  ) throws UpdateException {
    RealTimeTripTimes tripTimes = tripTimesWithStopPattern.tripTimes();
    Trip trip = tripTimes.getTrip();

    final StopPattern stopPattern = tripTimesWithStopPattern.stopPattern();
    final TripPattern pattern = tripPatternCache.getOrCreateTripPattern(
      stopPattern,
      trip,
      transitEditorService.findPattern(trip)
    );

    TripPattern hideTripInScheduledPattern = null;
    if (modified) {
      hideTripInScheduledPattern = getPatternForTripId(trip.getId());
    }

    var builder = RealTimeTripUpdate.of(pattern, tripTimes, serviceDate)
      .withRouteCreation(hasANewRouteBeenCreated)
      .withRevertPreviousRealTimeUpdates(true)
      .withHideTripInScheduledPattern(hideTripInScheduledPattern);
    if (added) {
      builder
        .withAddedTripOnServiceDate(
          TripOnServiceDate.of(trip.getId()).withTrip(trip).withServiceDate(serviceDate).build()
        )
        .withTripCreation(true);
    }
    return TripUpdateApplier.apply(buffer, builder.build());
  }

  /**
   * Remove any stop that is not known in the static transit data.
   */
  private List<StopAndStopTimeUpdate> matchStopsToStopTimeUpdates(TripUpdate tripUpdate) {
    return tripUpdate
      .stopTimeUpdates()
      .stream()
      .flatMap(st ->
        st
          .stopId()
          .flatMap(id -> {
            var stopId = new FeedScopedId(tripUpdate.tripId().getFeedId(), id);
            var stop = transitEditorService.getRegularStop(stopId);
            return Optional.ofNullable(stop).map(s -> new StopAndStopTimeUpdate(s, st));
          })
          .stream()
      )
      .toList();
  }

  private TripPattern getPatternForTripId(FeedScopedId tripId) {
    Trip trip = transitEditorService.getTrip(tripId);
    return transitEditorService.findPattern(trip);
  }
}
