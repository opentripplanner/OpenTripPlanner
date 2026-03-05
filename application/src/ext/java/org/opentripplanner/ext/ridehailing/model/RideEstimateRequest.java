package org.opentripplanner.ext.ridehailing.model;

import org.opentripplanner.street.geometry.WgsCoordinate;

public record RideEstimateRequest(
  WgsCoordinate startPosition,
  WgsCoordinate endPosition,
  boolean wheelchairAccessible
) {}
