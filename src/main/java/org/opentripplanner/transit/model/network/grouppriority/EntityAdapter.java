package org.opentripplanner.transit.model.network.grouppriority;

import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * These are the keys used to group transit trips and trip-patterns. This is used to calculate a
 * unique groupId based on the request config. We use the adapter pattern to be able to generate
 * the groupId based on different input types (TripPattern and Trip).
 */
interface EntityAdapter {
  TransitMode mode();
  String subMode();
  FeedScopedId agencyId();
  FeedScopedId routeId();
}
