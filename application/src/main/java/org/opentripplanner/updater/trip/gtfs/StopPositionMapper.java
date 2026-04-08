package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_STOP_REFERENCE;
import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_STOP_SEQUENCE;

import java.util.Collections;
import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.spi.UpdateException;
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
   */
  int stopPositionInPattern(int listIndex, StopTimeUpdate update) throws UpdateException {
    if (update.stopSequence().isPresent()) {
      return handleStopSequence(listIndex, update);
    } else if (update.stopId().isPresent()) {
      return handleStopId(listIndex, update.stopId().get());
    } else {
      throw UpdateException.of(tripId, INVALID_STOP_REFERENCE, listIndex);
    }
  }

  private int handleStopSequence(int listIndex, StopTimeUpdate update) throws UpdateException {
    var pos = tripTimes.stopPositionForGtfsSequence(update.stopSequence().getAsInt());
    if (pos.isEmpty()) {
      throw UpdateException.of(tripId, INVALID_STOP_SEQUENCE, listIndex);
    } else {
      return pos.getAsInt();
    }
  }

  private int handleStopId(int listIndex, String stopId) throws UpdateException {
    var visitsAtStop = Collections.frequency(stopIds, stopId);
    if (visitsAtStop == 0) {
      throw UpdateException.of(tripId, INVALID_STOP_REFERENCE, listIndex);
    } else if (visitsAtStop == 1) {
      return stopIds.indexOf(stopId);
    }
    // special case: circular stop pattern but the updates supplied contain only stop_id
    // not stop_sequence. it's quite questionable that this should be supported at all.
    else {
      return handleCircularRoute(listIndex, stopId);
    }
  }

  /**
   * Special handling for finding the stop position in the pattern when it is circular and the
   * update supplies only stop_id (not stop_sequence).
   *
   * We make a limited effort to interpret the data, but not every case is supported.
   */
  private int handleCircularRoute(int listIndex, String stopId) throws UpdateException {
    // we take the position of the update in the list and see if that, by chance, is the same
    // index in the pattern.
    if (stopIds.get(listIndex).equals(stopId)) {
      // order in the updates also happens to match the order in the stop pattern
      return listIndex;
    } else {
      // very niche edge case: the update supplies only stop_id and the beginning of
      // the trip is missing in the real-time update
      var lastPosInPattern = stopIds.lastIndexOf(stopId);
      if (lastPosInPattern == -1) {
        throw UpdateException.of(tripId, INVALID_STOP_REFERENCE, listIndex);
      } else {
        return lastPosInPattern;
      }
    }
  }
}
