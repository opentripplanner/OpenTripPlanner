package org.opentripplanner.transit.repository;

import java.time.LocalDate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;

public interface TimetableRepository extends TimetableRepositorySnapshot {
  void update(RealTimeTripUpdate realTimeTripUpdate);

  TimetableRepositorySnapshot createSnapshot();

  void clear(String feedId);

  boolean revertTripToScheduledTripPattern(FeedScopedId tripId, LocalDate serviceDate);

  boolean purgeExpiredData(LocalDate serviceDate);
}
