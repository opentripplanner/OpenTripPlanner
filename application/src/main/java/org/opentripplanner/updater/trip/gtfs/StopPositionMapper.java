package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;

import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.gtfs.model.StopTimeUpdate;

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

  Result<Integer, UpdateError> stopPositionInPattern(StopTimeUpdate update) {
    if (update.stopSequence().isPresent()) {
      var tmp = tripTimes.stopPositionForGtfsSequence(update.stopSequence().getAsInt());
      if (tmp.isEmpty()) {
        return Result.failure(new UpdateError(tripId, INVALID_STOP_SEQUENCE));
      } else {
        return Result.success(tmp.getAsInt());
      }
    } else if (update.stopId().isPresent()) {
      var i = stopIds.indexOf(update.stopId().get());
      return Result.success(i);
    } else {
      return Result.failure(new UpdateError(tripId, INVALID_STOP_SEQUENCE));
    }
  }
}
