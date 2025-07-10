package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_ARRIVAL_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;

import com.google.transit.realtime.GtfsRealtime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.gtfs.mapping.PickDropMapper;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripTimesPatch;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TripTimesUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(TripTimesUpdater.class);

  /**
   * Apply the TripUpdate to the appropriate TripTimes from a Timetable. The existing TripTimes
   * must not be modified directly because they may be shared with the underlying
   * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
   * protective copying of this Timetable. It is not done in this update method to avoid repeatedly
   * cloning the same Timetable when several updates are applied to it at once. We assume here that
   * all trips in a timetable are from the same feed, which should always be the case.
   *
   * @param tripUpdate                    GTFS-RT trip update
   * @param timeZone                      time zone of trip update
   * @param updateServiceDate             service date of trip update
   * @param backwardsDelayPropagationType Defines when delays are propagated to previous stops and
   *                                      if these stops are given the NO_DATA flag
   * @return {@link Result < TripTimesPatch ,    UpdateError   >} contains either a new copy of updated
   * TripTimes after TripUpdate has been applied on TripTimes of trip with the id specified in the
   * trip descriptor of the TripUpdate and a list of stop indices that have been skipped with the
   * realtime update; or an error if something went wrong
   */
  public static Result<TripTimesPatch, UpdateError> createUpdatedTripTimesFromGTFSRT(
    Timetable timetable,
    GtfsRealtime.TripUpdate tripUpdate,
    ZoneId timeZone,
    LocalDate updateServiceDate,
    ForwardsDelayPropagationType forwardsDelayPropagationType,
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    Result<TripTimesPatch, UpdateError> invalidInput = Result.failure(
      UpdateError.noTripId(INVALID_INPUT_STRUCTURE)
    );
    if (tripUpdate == null) {
      LOG.debug("A null TripUpdate pointer was passed to the Timetable class update method.");
      return invalidInput;
    }

    // Though all timetables have the same trip ordering, some may have extra trips due to
    // the dynamic addition of unscheduled trips.
    // However, we want to apply trip updates on top of *scheduled* times
    if (!tripUpdate.hasTrip()) {
      LOG.debug("TripUpdate object has no TripDescriptor field.");
      return invalidInput;
    }

    GtfsRealtime.TripDescriptor tripDescriptor = tripUpdate.getTrip();
    if (!tripDescriptor.hasTripId()) {
      LOG.debug("TripDescriptor object has no TripId field");
      return Result.failure(UpdateError.noTripId(TRIP_NOT_FOUND));
    }

    String tripId = tripDescriptor.getTripId();

    var feedScopedTripId = new FeedScopedId(timetable.getPattern().getFeedId(), tripId);

    var tripTimes = timetable.getTripTimes(feedScopedTripId);
    if (tripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", tripId);
      return Result.failure(new UpdateError(feedScopedTripId, TRIP_NOT_FOUND_IN_PATTERN));
    } else {
      LOG.trace("tripId {} found in timetable.", tripId);
    }

    RealTimeTripTimesBuilder builder = tripTimes.createRealTimeWithoutScheduledTimes();
    if (tripUpdate.hasTripProperties()) {
      var tripProperties = tripUpdate.getTripProperties();
      if (tripProperties.hasTripHeadsign()) {
        builder.withTripHeadsign(I18NString.of(tripProperties.getTripHeadsign()));
      }
      // TODO: add support for changing trip short name
    }

    Map<Integer, PickDrop> updatedPickups = new HashMap<>();
    Map<Integer, PickDrop> updatedDropoffs = new HashMap<>();
    Map<Integer, String> replacedStopIndices = new HashMap<>();

    // The GTFS-RT reference specifies that StopTimeUpdates are sorted by stop_sequence.
    Iterator<GtfsRealtime.TripUpdate.StopTimeUpdate> updates = tripUpdate
      .getStopTimeUpdateList()
      .iterator();
    GtfsRealtime.TripUpdate.StopTimeUpdate update = null;
    if (!updates.hasNext()) {
      LOG.warn("Zero-length trip update to trip {}.", tripId);
    } else {
      update = updates.next();
    }

    int numStops = tripTimes.getNumStops();

    final long today = ServiceDateUtils.asStartOfService(
      updateServiceDate,
      timeZone
    ).toEpochSecond();

    for (int i = 0; i < numStops; i++) {
      boolean match = false;
      if (update != null) {
        if (update.hasStopSequence()) {
          match = update.getStopSequence() == tripTimes.gtfsSequenceOfStopIndex(i);
        } else if (update.hasStopId()) {
          match = timetable.getPattern().getStop(i).getId().getId().equals(update.getStopId());
        }
      }

      if (match) {
        if (update.hasStopTimeProperties()) {
          var stopTimeProperties = update.getStopTimeProperties();
          if (stopTimeProperties.hasAssignedStopId()) {
            replacedStopIndices.put(i, stopTimeProperties.getAssignedStopId());
          }
          if (stopTimeProperties.hasPickupType()) {
            updatedPickups.put(
              i,
              PickDropMapper.map(stopTimeProperties.getPickupType().getNumber())
            );
          }
          if (stopTimeProperties.hasDropOffType()) {
            updatedDropoffs.put(
              i,
              PickDropMapper.map(stopTimeProperties.getDropOffType().getNumber())
            );
          }
          if (stopTimeProperties.hasStopHeadsign()) {
            builder.withStopHeadsign(i, I18NString.of(stopTimeProperties.getStopHeadsign()));
          }
        }

        GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship scheduleRelationship =
          update.hasScheduleRelationship()
            ? update.getScheduleRelationship()
            : GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED;
        // Handle each schedule relationship case
        if (
          scheduleRelationship ==
          GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED
        ) {
          // Set status to cancelled
          updatedPickups.put(i, PickDrop.CANCELLED);
          updatedDropoffs.put(i, PickDrop.CANCELLED);
          builder.withCanceled(i);
        } else if (
          scheduleRelationship ==
          GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA
        ) {
          // Set status to NO_DATA and delays to 0.
          // Note: GTFS-RT requires NO_DATA stops to have no arrival departure times.
          builder.withNoData(i);
        } else {
          // Else the status is SCHEDULED, update times as needed.
          if (update.hasArrival()) {
            GtfsRealtime.TripUpdate.StopTimeEvent arrival = update.getArrival();
            if (arrival.hasTime()) {
              builder.withArrivalTime(i, (int) (arrival.getTime() - today));
            } else if (arrival.hasDelay()) {
              builder.withArrivalDelay(i, arrival.getDelay());
            } else {
              LOG.debug(
                "Arrival time at index {} of trip {} has neither a delay nor a time.",
                i,
                feedScopedTripId
              );
              return Result.failure(new UpdateError(feedScopedTripId, INVALID_ARRIVAL_TIME, i));
            }
          }

          if (update.hasDeparture()) {
            GtfsRealtime.TripUpdate.StopTimeEvent departure = update.getDeparture();
            if (departure.hasTime()) {
              builder.withDepartureTime(i, (int) (departure.getTime() - today));
            } else if (departure.hasDelay()) {
              builder.withDepartureDelay(i, departure.getDelay());
            } else {
              LOG.debug(
                "Departure time at index {} of trip {} has neither a delay nor a time.",
                i,
                feedScopedTripId
              );
              return Result.failure(new UpdateError(feedScopedTripId, INVALID_DEPARTURE_TIME, i));
            }
          }
        }

        if (updates.hasNext()) {
          update = updates.next();
        } else {
          update = null;
        }
      }
    }
    if (update != null) {
      LOG.debug(
        "Part of a TripUpdate object could not be applied successfully to trip {}.",
        tripId
      );
      return Result.failure(new UpdateError(feedScopedTripId, INVALID_STOP_SEQUENCE));
    }

    // Interpolate missing times for stops which don't have times associated. Note: Currently for
    // GTFS-RT updates ONLY not for SIRI updates.
    if (
      ForwardsDelayInterpolator.getInstance(forwardsDelayPropagationType).interpolateDelay(builder)
    ) {
      LOG.debug("Interpolated delays for for missing stops on trip {}.", tripId);
    }

    var backwardPropagationIndex = BackwardsDelayInterpolator.getInstance(
      backwardsDelayPropagationType
    ).propagateBackwards(builder);
    backwardPropagationIndex.ifPresent(index ->
      LOG.debug("Propagated delay from stop index {} backwards on trip {}.", index, tripId)
    );

    if (tripUpdate.hasVehicle()) {
      var vehicleDescriptor = tripUpdate.getVehicle();
      if (vehicleDescriptor.hasWheelchairAccessible()) {
        GtfsRealtimeMapper.mapWheelchairAccessible(
          vehicleDescriptor.getWheelchairAccessible()
        ).ifPresent(builder::withWheelchairAccessibility);
      }
    }

    // Make sure that updated trip times have the correct real time state
    builder.withRealTimeState(RealTimeState.UPDATED);

    // Validate for non-increasing times. Log error if present.
    try {
      var result = builder.build();
      LOG.trace(
        "A valid TripUpdate object was applied to trip {} using the Timetable class update method.",
        tripId
      );
      return Result.success(
        new TripTimesPatch(result, updatedPickups, updatedDropoffs, replacedStopIndices)
      );
    } catch (DataValidationException e) {
      return DataValidationExceptionMapper.toResult(e);
    }
  }
}
