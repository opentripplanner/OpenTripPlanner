package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_REFERENCE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;

import java.util.Collections;
import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.gtfs.model.StopTimeUpdate;

/**
 * Takes a trip and a list of stop times and maps the stop times to the stop positions in the
 * trip's pattern.
 */
class StopPositionMapper {

  private final FeedScopedId tripId;
  private final TripTimes tripTimes;
  private final List<String> stopIds;

  StopPositionMapper(FeedScopedId tripId, TripTimes tripTimes, Timetable timetable) {
    this.tripId = tripId;
    this.tripTimes = tripTimes;
    this.stopIds = timetable
      .getPattern()
      .getStops()
      .stream()
      .map(s -> s.getId().getId())
      .toList();
  }

  /**
   * Takes a stop time update and the index of the stop time in the list of stop times and returns
   * the stop position in the trip's pattern.
   *
   * @param listIndex The list index of the update in the list of stop time updates
   * @return
   */
  Result<Integer, UpdateError> stopPositionInPattern(StopTimeUpdate update, int listIndex) {
    if (update.stopSequence().isPresent()) {
      return handleStopSequence(update, listIndex);
    } else if (update.stopId().isPresent()) {
      return handleStopId(listIndex, update.stopId().get());
    } else {
      return invalid(listIndex);
    }
  }

  private Result<Integer, UpdateError> handleStopSequence(StopTimeUpdate update, int listIndex) {
    var tmp = tripTimes.stopPositionForGtfsSequence(update.stopSequence().getAsInt());
    if (tmp.isEmpty()) {
      return Result.failure(new UpdateError(tripId, INVALID_STOP_SEQUENCE, listIndex));
    } else {
      return Result.success(tmp.getAsInt());
    }
  }

  private Result<Integer, UpdateError> handleStopId(int listIndex, String stopId) {
    var visitsAtStop = Collections.frequency(stopIds, stopId);
    if (visitsAtStop == 0) {
      return invalid(listIndex);
    } else if (visitsAtStop == 1) {
      var i = stopIds.indexOf(stopId);
      return Result.success(i);
    }
    // special case: circular stop pattern but the updates supplied contain only stop_id
    // not stop_sequence. it's quite questionable that this should be supported at all.
    if (visitsAtStop > 1) {
      return handleCircularRoute(listIndex, stopId);
    }
    return invalid(listIndex);
  }

  /**
   * Special handling for finding the stop position in the pattern when it is circular and the
   * update supplies only stop_id.
   */
  private Result<Integer, UpdateError> handleCircularRoute(int listIndex, String stopId) {
    // we take the position of the update in the list and see if that by chance is the same
    // index the pattern.
    if (stopIds.get(listIndex).equals(stopId)) {
      // order in the updates also happens to match the order in the stop pattern
      return Result.success(listIndex);
    } else {
      // very niche edge case: the stop pattern is circular, the update supplies only stop_id
      // and the beginning of the trip is missing in the real-time update
      var lastPosInPattern = stopIds.lastIndexOf(stopId);
      if (lastPosInPattern == -1) {
        return invalid(listIndex);
      } else {
        return Result.success(lastPosInPattern);
      }
    }
  }

  private Result<Integer, UpdateError> invalid(int listIndex) {
    return Result.failure(new UpdateError(tripId, INVALID_STOP_REFERENCE, listIndex));
  }
}
