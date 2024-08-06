package org.opentripplanner.routing.api.request;

/**
 * Raptor check paths in 3 different places. The debugger can print events
 * for each of these.
 */
public enum DebugEventType {
  STOP_ARRIVALS,
  PATTERN_RIDES,
  DESTINATION_ARRIVALS,
}
