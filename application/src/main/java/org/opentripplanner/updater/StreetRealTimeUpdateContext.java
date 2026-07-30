package org.opentripplanner.updater;

import org.opentripplanner.street.graph.Graph;

/**
 * Give access to the street model in the context of a real-time update task in the street write
 * domain. The street model must be mutated only from the street domain's writer thread.
 */
public interface StreetRealTimeUpdateContext {
  /**
   * Return the street model (graph).
   */
  Graph graph();
}
