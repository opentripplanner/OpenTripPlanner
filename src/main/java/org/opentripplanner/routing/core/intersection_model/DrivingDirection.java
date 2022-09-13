package org.opentripplanner.routing.core.intersection_model;

import java.util.HashMap;
import java.util.Map;

public enum DrivingDirection {
  /**
   * Specifies that cars go on the right hand side of the road. This is true for the US mainland
   * Europe.
   */
  RIGHT_HAND_TRAFFIC("right"),
  /**
   * Specifies that cars go on the left hand side of the road. This is true for the UK, Japan and
   * Australia.
   */
  LEFT_HAND_TRAFFIC("left");

  private static final Map<String, DrivingDirection> lookup = new HashMap<>();

  private final String configName;

  static {
    for (DrivingDirection s : DrivingDirection.values()) {
      lookup.put(s.configName, s);
    }
  }

  DrivingDirection(String configName) {
    this.configName = configName;
  }

  public String configName() {
    return configName;
  }

  public static DrivingDirection get(String configName) {
    return lookup.get(configName);
  }
}
