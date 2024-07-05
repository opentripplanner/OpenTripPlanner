package org.opentripplanner.routing.api.request;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Defines one pass-through point which the journey must pass through.
 */
public record PassThroughPoint(List<StopLocation> stopLocations, @Nullable String name) {
  /**
   * Get the one or multiple stops of the pass-through point, of which only one is required to be
   * passed through.
   */
  @Override
  public List<StopLocation> stopLocations() {
    return stopLocations;
  }

  /**
   * Get an optional name of the pass-through point for debugging and logging.
   */
  @Override
  @Nullable
  public String name() {
    return name;
  }
}
