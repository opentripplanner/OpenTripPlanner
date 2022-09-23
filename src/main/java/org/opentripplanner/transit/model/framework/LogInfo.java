package org.opentripplanner.transit.model.framework;

import javax.annotation.Nullable;

/** Implement this interface to include more info into the {@link AbstractTransitEntity#toString()}. */
public interface LogInfo {
  @Nullable
  String logName();
}
