package org.opentripplanner.ext.empiricaldelay.internal.csvinput.delay;

import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * An emperical dealy trip time csv file row.
 */
public record TripTimeDelayRow(
  String empiricalDelayServiceId,
  FeedScopedId tripId,
  FeedScopedId stopId,
  int stopSequence,
  int p50,
  int p90
) {}
