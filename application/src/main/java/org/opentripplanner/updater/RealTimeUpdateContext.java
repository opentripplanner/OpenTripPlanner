package org.opentripplanner.updater;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.trip.siri.EntityResolver;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatcher;

/**
 * Give access to the transit data and street model in the context of a real-time updater.
 * The services exposed should be used only from the GraphWriter thread.
 */
public interface RealTimeUpdateContext {
  /**
   * Return the street model (graph).
   */
  Graph graph();

  /**
   * Return a transit service that can look up both scheduled and real-time data.
   * The transit service has access to all real-time updates applied so far,
   * including those not yet committed in a published snapshot.
   */
  TransitService transitService();

  /**
   * Return a SIRI fuzzy trip matcher that can look up both scheduled and real-time data.
   * The SIRI fuzzy trip matcher has access to all real-time updates applied so far,
   * including those not yet committed in a published snapshot.
   */
  SiriFuzzyTripMatcher siriFuzzyTripMatcher();

  /**
   * Return a GTFS-RT fuzzy trip matcher that can look up both scheduled and real-time data.
   * The GTFS-RT fuzzy trip matcher has access to all real-time updates applied so far,
   * including those not yet committed in a published snapshot.
   */
  GtfsRealtimeFuzzyTripMatcher gtfsRealtimeFuzzyTripMatcher();

  /**
   * Return an entity resolver that can look up both scheduled and real-time data.
   * The entity resolver has access to all real-time updates applied so far,
   * including those not yet committed in a published snapshot.
   */
  EntityResolver entityResolver(String feedId);
}
