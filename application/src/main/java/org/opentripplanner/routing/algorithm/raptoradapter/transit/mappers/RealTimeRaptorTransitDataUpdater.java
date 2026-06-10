package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Function;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.TimetableSnapshotUpdateListener;
import org.opentripplanner.transit.service.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update the RaptorTransitData from a set of TimeTables. A shallow copy is made of the RaptorTransitData
 * (this also includes a shallow copy of the TripPatternsForDate map). TripPatterns are matched on
 * id and replaced by their updated versions. The realtime RaptorTransitData is then switched out with
 * the updated copy in an atomic operation. This ensures that any RaptorTransitData that is referenced
 * from the Graph is never changed.
 *
 * This is a way of keeping the RaptorTransitData up to date (in sync with the TimetableRepository plus its most
 * recent TimetableSnapshot) without repeatedly deriving it from scratch every few seconds. The same
 * incremental changes are applied to both the TimetableSnapshot and the RaptorTransitData and they are
 * published together.
 */
public class RealTimeRaptorTransitDataUpdater implements TimetableSnapshotUpdateListener {

  private static final Logger LOG = LoggerFactory.getLogger(RealTimeRaptorTransitDataUpdater.class);

  private final TimetableRepository timetableRepository;
  private final TimetableUpdateMapper timetableUpdateMapper;

  public RealTimeRaptorTransitDataUpdater(TimetableRepository timetableRepository) {
    this.timetableRepository = timetableRepository;
    this.timetableUpdateMapper = new TimetableUpdateMapper();
  }

  @Override
  public void update(
    Collection<Timetable> updatedTimetables,
    Function<FeedScopedId, SortedSet<Timetable>> timetableProvider
  ) {
    if (!timetableRepository.hasRealtimeRaptorTransitData()) {
      return;
    }

    long startTime = System.currentTimeMillis();

    // Instantiate a TripPatternForDateMapper with the new TripPattern mappings
    TripPatternForDateMapper tripPatternForDateMapper = new TripPatternForDateMapper(
      timetableRepository.getServiceCodesRunningForDate()
    );

    RaptorTransitData realtimeRaptorTransitData = timetableUpdateMapper.map(
      timetableRepository.getRealtimeRaptorTransitData(),
      updatedTimetables,
      timetableProvider,
      tripPatternForDateMapper
    );

    // Switch out the reference with the updated realtimeRaptorTransitData. This is synchronized to
    // guarantee that the reference is set after all the fields have been updated.
    timetableRepository.setRealtimeRaptorTransitData(realtimeRaptorTransitData);

    LOG.debug(
      "UPDATING {} tripPatterns took {} ms",
      updatedTimetables.size(),
      System.currentTimeMillis() - startTime
    );
  }
}
