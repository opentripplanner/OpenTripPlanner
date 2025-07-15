package org.opentripplanner.street.model;

import jakarta.inject.Inject;
import java.io.Serializable;

/**
 * Holds limits of the street graph.
 * <p>
 * TODO this can be expanded to include some fields from the {@link org.opentripplanner.routing.graph.Graph}.
 */
public class StreetLimitationParameters implements Serializable {

  private float maxCarSpeed = StreetConstants.DEFAULT_MAX_CAR_SPEED;
  private int maxAreaNodes = StreetConstants.DEFAULT_MAX_AREA_NODES;

  @Inject
  public StreetLimitationParameters() {}

  /**
   * Initiliaze the maximum speed limit in m/s.
   */
  public void initMaxCarSpeed(float maxCarSpeed) {
    this.maxCarSpeed = maxCarSpeed;
  }

  /**
   * If this graph contains car routable streets, this value is the maximum speed limit in m/s.
   * Defaults to 40 m/s == 144 km/h.
   */
  public float maxCarSpeed() {
    return maxCarSpeed;
  }

  /**
   * Initialize limit for area linking
   */
  public void initMaxAreaNodes(int maxAreaNodes) {
    this.maxAreaNodes = maxAreaNodes;
  }

  /**
   * Get the limit for area linking
   */
  public int maxAreaNodes() {
    return maxAreaNodes;
  }
}
