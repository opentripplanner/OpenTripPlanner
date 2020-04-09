package org.opentripplanner.model;

public interface StationElement {

  /** Get the level name for the station element */
  abstract String getLevelName();

  /** Get the relative level inside the station. Used eg. for calculating elevator hops */
  abstract double getLevelIndex();
}
