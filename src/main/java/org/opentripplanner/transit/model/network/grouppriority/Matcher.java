package org.opentripplanner.transit.model.network.grouppriority;

import org.opentripplanner.transit.model.network.TripPattern;

interface Matcher {
  boolean match(TripPattern pattern);

  default boolean isEmpty() {
    return false;
  }
}
