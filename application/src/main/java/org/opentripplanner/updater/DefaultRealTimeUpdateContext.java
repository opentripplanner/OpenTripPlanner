package org.opentripplanner.updater;

import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.trip.siri.EntityResolver;

public class DefaultRealTimeUpdateContext implements RealTimeUpdateContext {

  private final Graph graph;
  private final TimetableRepository timetableRepository;
  private final TransitService transitService;

  /**
   * The context needs the mutable repository so that entity lookups (trips, routes, patterns) see
   * all in-progress real-time additions that have not yet been committed to a published snapshot.
   * <p>
   * A {@link TimetableRepository} cannot be used directly for these lookups, because every
   * lookup must also fall back to scheduled data in the {@link TransitRepository} when an entity
   * is not found in the real-time repository. The {@link DefaultTransitService} combines both: it
   * checks the repository first, then falls back to the static index.
   * <p>
   * {@link DefaultTransitService} accepts a {@link org.opentripplanner.transit.repository.TimetableRepositorySnapshot},
   * because in request scope it must never receive the mutable repository. Passing the repository
   * here is safe because {@link TimetableRepository} extends
   * {@link org.opentripplanner.transit.repository.TimetableRepositorySnapshot}. A cleaner separation
   * would require merging scheduled and real-time data into a single unified store - this is the end goal!
   */
  public DefaultRealTimeUpdateContext(
    Graph graph,
    TransitRepository transitRepository,
    TimetableRepository timetableRepository
  ) {
    this.graph = graph;
    this.timetableRepository = timetableRepository;
    this.transitService = new DefaultTransitService(transitRepository, timetableRepository);
  }

  /**
   * Constructor for unit tests only.
   */
  public DefaultRealTimeUpdateContext(Graph graph, TransitRepository transitRepository) {
    this(graph, transitRepository, null);
  }

  @Override
  public TimetableRepository timetableRepository() {
    return timetableRepository;
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public TransitService transitService() {
    return transitService;
  }

  @Override
  public GtfsRealtimeFuzzyTripMatcher gtfsRealtimeFuzzyTripMatcher() {
    return new GtfsRealtimeFuzzyTripMatcher(transitService);
  }

  @Override
  public EntityResolver entityResolver(String feedId) {
    return new EntityResolver(transitService, feedId);
  }
}
