package org.opentripplanner.model;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Some stopTimes all in the same pattern.
 */
public class StopTimesInPattern {

  public TripPattern pattern;
  public List<TripTimeOnDate> times = new ArrayList<>();

  public StopTimesInPattern(TripPattern pattern) {
    this.pattern = pattern;
  }
}
