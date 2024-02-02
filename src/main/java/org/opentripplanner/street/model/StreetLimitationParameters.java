package org.opentripplanner.street.model;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.Serializable;
import javax.annotation.Nullable;

/**
 * Holds limits of the street graph.
 * <p>
 * TODO this can be expanded to include some fields from the {@link org.opentripplanner.routing.graph.Graph}.
 */
@Singleton
public class StreetLimitationParameters implements Serializable {

  private Float maxCarSpeed = null;

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
   */
  @Nullable
  public Float maxCarSpeed() {
    return maxCarSpeed;
  }
}
