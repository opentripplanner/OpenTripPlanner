package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_STOP_REFERENCE;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.spi.UpdateErrorType.OUTSIDE_SERVICE_PERIOD;
import static org.opentripplanner.updater.spi.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_ALREADY_EXISTS;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateErrorType.UNKNOWN_STOP;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TripUpdateApplier;
import org.opentripplanner.updater.trip.gtfs.model.StopTimeUpdate;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;

/**
 * Handles GTFS-RT TripUpdates for trips with schedule relationship {@code NEW}, {@code ADDED}, or
 * {@code REPLACEMENT}. Builds a new {@link org.opentripplanner.transit.model.timetable.Trip} (and
 * route, if needed) from the feed message and resolves its stop-time updates to known stops.
 */
class NewTripHandler {

  private final TransitService transitService;
  private final TimetableRepository buffer;
  private final TripTimesUpdater tripTimesUpdater;
  private final TripPatternCache tripPatternCache;

  NewTripHandler(
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

  /**
   * Validate and handle GTFS-RT TripUpdate message containing a NEW trip.
   */
  UpdateSuccess handleNew(final TripUpdate tripUpdate) throws UpdateException {
    if (transitService.getScheduledTrip(tripUpdate.tripId()) != null) {
      throw UpdateException.of(tripUpdate.tripId(), TRIP_ALREADY_EXISTS);
    }
    var serviceId = transitService.getOrCreateServiceIdForDate(tripUpdate.startDate());
    if (serviceId == null) {
      throw UpdateException.of(tripUpdate.tripId(), OUTSIDE_SERVICE_PERIOD);
    }

    var result = new RouteFactory(transitService).getOrCreate(tripUpdate);

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
    Trip trip = transitService.getTrip(tripUpdate.tripId());

    if (trip == null) {
      throw UpdateException.of(tripUpdate.tripId(), TRIP_NOT_FOUND);
    }

    final Set<FeedScopedId> serviceIds = transitService
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
    var stopTimeUpdates = tripUpdate.stopTimeUpdates();

    if (stopTimeUpdates.size() < 2) {
      throw UpdateException.of(tripId, TOO_FEW_STOPS);
    }

    var stopAndStopTimeUpdates = resolveStops(tripId, stopTimeUpdates);

    var value = tripTimesUpdater.createNewTripTimesFromGtfsRt(
      trip,
      tripUpdate,
      stopAndStopTimeUpdates,
      added,
      modified,
      transitService.getTripCalendars().getServiceCode(trip.getServiceId())
    );

    return addNewOrReplacementTripToSnapshot(
      value,
      tripUpdate.startDate(),
      added,
      modified,
      hasANewRouteBeenCreated
    );
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
      transitService.findPattern(trip)
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
   * Resolve the stop of every stop time update against the site repository.
   * <p>
   * The whole update is rejected if a single stop cannot be resolved.
   *
   * @throws UpdateException {@code INVALID_STOP_REFERENCE} if a stop time update has no stop id -
   *                         a new trip has no pattern yet, so a stop sequence alone cannot be
   *                         resolved to a stop - or {@code UNKNOWN_STOP} if the stop id is not
   *                         present in the site repository.
   */
  private List<StopAndStopTimeUpdate> resolveStops(
    FeedScopedId tripId,
    List<StopTimeUpdate> stopTimeUpdates
  ) throws UpdateException {
    var stops = new ArrayList<StopAndStopTimeUpdate>(stopTimeUpdates.size());
    for (int listIndex = 0; listIndex < stopTimeUpdates.size(); listIndex++) {
      var stopTimeUpdate = stopTimeUpdates.get(listIndex);
      var stopId = stopTimeUpdate.stopId();
      if (stopId.isEmpty()) {
        throw UpdateException.of(tripId, INVALID_STOP_REFERENCE, listIndex);
      }
      var stop = transitService.getRegularStop(new FeedScopedId(tripId.getFeedId(), stopId.get()));
      if (stop == null) {
        throw UpdateException.of(tripId, UNKNOWN_STOP, listIndex);
      }
      stops.add(new StopAndStopTimeUpdate(stop, stopTimeUpdate));
    }
    return stops;
  }

  private TripPattern getPatternForTripId(FeedScopedId tripId) {
    Trip trip = transitService.getTrip(tripId);
    return transitService.findPattern(trip);
  }
}
