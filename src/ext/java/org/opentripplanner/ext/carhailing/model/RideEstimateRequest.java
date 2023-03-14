package org.opentripplanner.ext.carhailing.model;

import org.opentripplanner.framework.geometry.WgsCoordinate;

public record RideEstimateRequest(WgsCoordinate startPosition, WgsCoordinate endPosition) {}
