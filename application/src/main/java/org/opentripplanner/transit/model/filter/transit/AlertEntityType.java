package org.opentripplanner.transit.model.filter.transit;

/**
 * The type of entity affected by an alert, used when filtering alerts by the entities they affect.
 * <p>
 * The type depends on what the entity resolves to in the transit data: an entity which doesn't
 * resolve to anything, for example a route which doesn't exist, is {@link #UNKNOWN}.
 */
public enum AlertEntityType {
  AGENCY,
  PATTERN,
  ROUTE,
  ROUTE_TYPE,
  STOP,
  STOP_ON_ROUTE,
  STOP_ON_TRIP,
  TRIP,
  UNKNOWN,
}
