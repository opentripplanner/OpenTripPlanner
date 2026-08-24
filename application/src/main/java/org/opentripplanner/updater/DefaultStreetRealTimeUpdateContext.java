package org.opentripplanner.updater;

import org.opentripplanner.street.graph.Graph;

/**
 * Default implementation of {@link StreetRealTimeUpdateContext}.
 */
public record DefaultStreetRealTimeUpdateContext(Graph graph) implements
  StreetRealTimeUpdateContext {}
