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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
      Result.failure(UpdateError.noTripId(TRIP_NOT_FOUND));
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

    RealTimeTripTimesBuilder builder = tripTimes.createRealTimeFromScheduledTimes();
    List<Integer> skippedStopIndices = new ArrayList<>();

    // The GTFS-RT reference specifies that StopTimeUpdates are sorted by stop_sequence.
    Iterator<GtfsRealtime.TripUpdate.StopTimeUpdate> updates = tripUpdate
      .getStopTimeUpdateList()
      .iterator();
    if (!updates.hasNext()) {
      LOG.warn("Won't apply zero-length trip update to trip {}.", tripId);
      return Result.failure(new UpdateError(feedScopedTripId, TOO_FEW_STOPS));
    }
    GtfsRealtime.TripUpdate.StopTimeUpdate update = updates.next();

    int numStops = tripTimes.getNumStops();
    Integer delay = null;
    Integer firstUpdatedIndex = null;

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
        GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship scheduleRelationship =
          update.hasScheduleRelationship()
            ? update.getScheduleRelationship()
            : GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED;
        // Handle each schedule relationship case
        if (
          scheduleRelationship ==
          GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED
        ) {
          // Set status to cancelled and delays to previously recorded delays or to 0 otherwise.
          // Note: This will discard the times from TripUpdates even if they are present.
          skippedStopIndices.add(i);
          builder.withCanceled(i);
          int delayOrZero = delay != null ? delay : 0;
          builder.withArrivalDelay(i, delayOrZero);
          builder.withDepartureDelay(i, delayOrZero);
        } else if (
          scheduleRelationship ==
          GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.NO_DATA
        ) {
          // Set status to NO_DATA and delays to 0.
          // Note: GTFS-RT requires NO_DATA stops to have no arrival departure times.
          builder.withArrivalDelay(i, 0);
          builder.withDepartureDelay(i, 0);
          delay = 0;
          builder.withNoData(i);
        } else {
          // Else the status is SCHEDULED, update times as needed.
          if (update.hasArrival()) {
            if (firstUpdatedIndex == null) {
              firstUpdatedIndex = i;
            }
            GtfsRealtime.TripUpdate.StopTimeEvent arrival = update.getArrival();
            if (arrival.hasDelay()) {
              delay = arrival.getDelay();
              if (arrival.hasTime()) {
                builder.withArrivalTime(i, (int) (arrival.getTime() - today));
              } else {
                builder.withArrivalDelay(i, delay);
              }
            } else if (arrival.hasTime()) {
              builder.withArrivalTime(i, (int) (arrival.getTime() - today));
              delay = builder.getArrivalDelay(i);
            } else {
              LOG.debug(
                "Arrival time at index {} of trip {} has neither a delay nor a time.",
                i,
                feedScopedTripId
              );
              return Result.failure(new UpdateError(feedScopedTripId, INVALID_ARRIVAL_TIME, i));
            }
          } else if (delay != null) {
            builder.withArrivalDelay(i, delay);
          }

          if (update.hasDeparture()) {
            if (firstUpdatedIndex == null) {
              firstUpdatedIndex = i;
            }
            GtfsRealtime.TripUpdate.StopTimeEvent departure = update.getDeparture();
            if (departure.hasDelay()) {
              delay = departure.getDelay();
              if (departure.hasTime()) {
                builder.withDepartureTime(i, (int) (departure.getTime() - today));
              } else {
                builder.withDepartureDelay(i, delay);
              }
            } else if (departure.hasTime()) {
              builder.withDepartureTime(i, (int) (departure.getTime() - today));
              delay = builder.getDepartureDelay(i);
            } else {
              LOG.debug(
                "Departure time at index {} of trip {} has neither a delay nor a time.",
                i,
                feedScopedTripId
              );
              return Result.failure(new UpdateError(feedScopedTripId, INVALID_DEPARTURE_TIME, i));
            }
          } else if (delay != null) {
            builder.withDepartureDelay(i, delay);
          }
        }

        if (updates.hasNext()) {
          update = updates.next();
        } else {
          update = null;
        }
      } else if (delay != null) {
        // If not match and has previously set delays, propagate delays.
        builder.withArrivalDelay(i, delay);
        builder.withDepartureDelay(i, delay);
      }
    }
    if (update != null) {
      LOG.debug(
        "Part of a TripUpdate object could not be applied successfully to trip {}.",
        tripId
      );
      return Result.failure(new UpdateError(feedScopedTripId, INVALID_STOP_SEQUENCE));
    }

    // Interpolate missing times from SKIPPED stops since they don't necessarily have times
    // associated. Note: Currently for GTFS-RT updates ONLY not for SIRI updates.
    if (builder.interpolateMissingTimes()) {
      LOG.debug("Interpolated delays for cancelled stops on trip {}.", tripId);
    }

    // Backwards propagation for past stops that are no longer present in GTFS-RT, that is, up until
    // the first SCHEDULED stop sequence included in the GTFS-RT feed.
    if (firstUpdatedIndex != null && firstUpdatedIndex > 0) {
      if (
        BackwardsDelayPropagator.getBackwardsDelayPropagator(
          backwardsDelayPropagationType
        ).adjustTimes(builder, firstUpdatedIndex)
      ) {
        LOG.debug(
          "Propagated delay from stop index {} backwards on trip {}.",
          firstUpdatedIndex,
          tripId
        );
      }
    }

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
      return Result.success(new TripTimesPatch(result, skippedStopIndices));
    } catch (DataValidationException e) {
      return DataValidationExceptionMapper.toResult(e);
    }
  }
}
