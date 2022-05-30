package org.opentripplanner.transit.model.framework;

/** Implement this interface to include more info into the {@link TransitEntity#toString()}. */
public interface LogInfo {
  String logName();
}
