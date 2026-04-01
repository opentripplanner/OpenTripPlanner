package org.opentripplanner.street.model;

import java.io.Serializable;
import org.opentripplanner.street.graph.Graph;

/**
 * Container for street model information calculated during graph build.
 * <p>
 * TODO this can be expanded to include some fields from the {@link Graph}.
 */
public class StreetModelDetails implements Serializable {

  private final Float maxCarSpeed;
  private final Integer maxAreaNodes;

  public static final StreetModelDetails DEFAULT = new StreetModelDetails(
    StreetConstants.DEFAULT_MAX_CAR_SPEED,
    StreetConstants.DEFAULT_MAX_AREA_NODES
  );

  public StreetModelDetails(Float maxCarSpeed, Integer maxAreaNodes) {
    this.maxCarSpeed = maxCarSpeed;
    this.maxAreaNodes = maxAreaNodes;
  }

  /**
   * If this graph contains car routable streets, this value is the maximum speed limit in m/s.
   * Defaults to 40 m/s == 144 km/h.
   */
  public float maxCarSpeed() {
    return maxCarSpeed;
  }

  /**
   * Get the limit for area linking
   */
  public int maxAreaNodes() {
    return maxAreaNodes;
  }
}
