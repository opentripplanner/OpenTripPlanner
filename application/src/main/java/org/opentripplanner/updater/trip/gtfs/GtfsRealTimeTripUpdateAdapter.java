package org.opentripplanner.updater.trip.gtfs;

import java.time.LocalDate;
import java.util.function.Supplier;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;
import org.opentripplanner.updater.trip.patterncache.TripPatternIdGenerator;

/**
 * Application-scoped factory for GTFS-RT trip update processing. Holds stable, application-lifetime
 * state and produces a per-task {@link GtfsRealTimeUpdateHandler} via {@link #forUpdate(TimetableRepository)}.
 */
public class GtfsRealTimeTripUpdateAdapter {

  private final TransitRepository transitRepository;
  private final Supplier<LocalDate> localDateNow;
  private final TripPatternCache tripPatternCache;
  private final TripTimesUpdater tripTimesUpdater;
  private final DeduplicatorService deduplicator;

  /**
   * Constructor to allow tests to provide their own clock, not using system time.
   */
  public GtfsRealTimeTripUpdateAdapter(
    TransitRepository transitRepository,
    DeduplicatorService deduplicator,
    Supplier<LocalDate> localDateNow
  ) {
    this.transitRepository = transitRepository;
    this.localDateNow = localDateNow;
    this.tripPatternCache = new TripPatternCache(new TripPatternIdGenerator());
    this.tripTimesUpdater = new TripTimesUpdater(transitRepository.getTimeZone(), deduplicator);
    this.deduplicator = deduplicator;
  }

  /**
   * Create an update-scoped task for applying GTFS-RT trip updates. The task holds sub-handlers
   * backed by a {@link TransitEditorService} constructed from
   * the given buffer, so all pattern and trip lookups within the task see in-progress real-time
   * additions.
   */
  public GtfsRealTimeUpdateHandler forUpdate(TimetableRepository buffer) {
    var editorService = new DefaultTransitService(transitRepository, buffer);
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
