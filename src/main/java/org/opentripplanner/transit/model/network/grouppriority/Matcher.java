package org.opentripplanner.transit.model.network.grouppriority;

interface Matcher {
  boolean match(EntityAdapter entity);

  default boolean isEmpty() {
    return false;
  }
}
