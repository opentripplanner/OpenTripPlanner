package org.opentripplanner.apis.gtfs.model;

import org.opentripplanner.transit.model.network.TripPattern;

public record CanceledTripsSummaryPattern(TripPattern pattern, long cancellationCount) {}
