package org.opentripplanner.ext.ridehailing.model;

import org.opentripplanner.framework.geometry.WgsCoordinate;

public record RideEstimateRequest(
  WgsCoordinate startPosition,
  WgsCoordinate endPosition,
  boolean wheelchairAccessible
) {}
