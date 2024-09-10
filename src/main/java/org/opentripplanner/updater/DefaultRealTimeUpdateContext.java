package org.opentripplanner.updater;

import org.opentripplanner.ext.siri.EntityResolver;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;

public class DefaultRealTimeUpdateContext implements RealTimeUpdateContext {

  private final Graph graph;
  private final TransitService transitService;
  private SiriFuzzyTripMatcher siriFuzzyTripMatcher;

  public DefaultRealTimeUpdateContext(
    Graph graph,
    TransitModel transitModel,
    TimetableSnapshot timetableSnapshotBuffer
  ) {
    this.graph = graph;
    this.transitService = new DefaultTransitService(transitModel, timetableSnapshotBuffer);
  }

  /**
   * Constructor for unit tests only.
   */
  public DefaultRealTimeUpdateContext(Graph graph, TransitModel transitModel) {
    this(graph, transitModel, null);
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
  public synchronized SiriFuzzyTripMatcher siriFuzzyTripMatcher() {
    if (siriFuzzyTripMatcher == null) {
      siriFuzzyTripMatcher = new SiriFuzzyTripMatcher(transitService);
    }
    return siriFuzzyTripMatcher;
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
