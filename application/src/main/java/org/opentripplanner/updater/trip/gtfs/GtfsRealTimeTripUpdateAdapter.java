package org.opentripplanner.updater.trip.gtfs;

import java.time.LocalDate;
import java.util.function.Supplier;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.repository.MutableTimetableSnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;
import org.opentripplanner.updater.trip.patterncache.TripPatternIdGenerator;

/**
 * Application-scoped factory for GTFS-RT trip update processing. Holds stable, application-lifetime
 * state and produces a per-task {@link GtfsRealTimeUpdateHandler} via {@link #forUpdate(MutableTimetableSnapshot)}.
 */
public class GtfsRealTimeTripUpdateAdapter {

  private final TimetableRepository timetableRepository;
  private final Supplier<LocalDate> localDateNow;
  private final TripPatternCache tripPatternCache;
  private final TripTimesUpdater tripTimesUpdater;
  private final DeduplicatorService deduplicator;

  /**
   * Constructor to allow tests to provide their own clock, not using system time.
   */
  public GtfsRealTimeTripUpdateAdapter(
    TimetableRepository timetableRepository,
    DeduplicatorService deduplicator,
    Supplier<LocalDate> localDateNow
  ) {
    this.timetableRepository = timetableRepository;
    this.localDateNow = localDateNow;
    this.tripPatternCache = new TripPatternCache(new TripPatternIdGenerator());
    this.tripTimesUpdater = new TripTimesUpdater(timetableRepository.getTimeZone(), deduplicator);
    this.deduplicator = deduplicator;
  }

  /**
   * Create an update-scoped task for applying GTFS-RT trip updates. The task holds sub-handlers
   * backed by a {@link TransitEditorService} constructed from
   * the given buffer, so all pattern and trip lookups within the task see in-progress real-time
   * additions.
   */
  public GtfsRealTimeUpdateHandler forUpdate(MutableTimetableSnapshot buffer) {
    var editorService = new DefaultTransitService(timetableRepository, buffer);
    return new GtfsRealTimeUpdateHandler(
      buffer,
      localDateNow,
      new ScheduledTripHandler(editorService, buffer, tripTimesUpdater, tripPatternCache),
      new NewTripHandler(editorService, buffer, tripTimesUpdater, tripPatternCache),
      new CanceledTripHandler(editorService, buffer),
      new DuplicatedTripHandler(editorService, buffer, deduplicator)
    );
  }
}
