package org.opentripplanner.ext.carhailing.service;

import org.opentripplanner.framework.geometry.WgsCoordinate;

public record RideEstimateRequest(WgsCoordinate startPosition, WgsCoordinate endPosition) {}
