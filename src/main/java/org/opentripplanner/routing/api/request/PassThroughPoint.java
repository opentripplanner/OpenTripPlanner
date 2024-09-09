package org.opentripplanner.routing.api.request;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Defines one pass-through point which the journey must pass through.
 */
public record PassThroughPoint(@Nullable String label, List<StopLocation> locations) {
  /**
   * Get the one or multiple stops of the pass-through point, of which only one is required to be
   * passed through.
   */
  @Override
  public List<StopLocation> locations() {
    return locations;
  }

  /**
   * Get an optional name of the pass-through point for debugging and logging.
   */
  @Override
  @Nullable
  public String label() {
    return label;
  }
}
