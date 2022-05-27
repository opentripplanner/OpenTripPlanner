package org.opentripplanner.transit.model.basic;

import org.opentripplanner.transit.model.basic.TransitEntity;

/** Implement this interface to include more info into the {@link TransitEntity#toString()}. */
public interface LogInfo {
  String logName();
}
