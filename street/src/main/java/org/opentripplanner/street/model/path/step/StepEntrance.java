package org.opentripplanner.street.model.path.step;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.model.vertex.StationEntranceVertex;

/**
 * A reference to a station entrance or exit used in a {@link StreetStep}. The street model does not
 * contain all information about an entrance, so it is up to the caller to resolve it.
 */
public sealed interface StepEntrance {
  /**
   * An entrance which is part of the transit model, identified by its id.
   */
  record TransitEntrance(FeedScopedId id) implements StepEntrance {}

  /**
   * An entrance which only exists in the street graph, typically imported from OSM.
   */
  record StationEntrance(StationEntranceVertex vertex) implements StepEntrance {}
}
