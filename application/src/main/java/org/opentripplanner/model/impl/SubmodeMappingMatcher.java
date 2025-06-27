package org.opentripplanner.model.impl;

import org.opentripplanner.model.FeedType;

/**
 * The key part of a row in submode-mapping.csv, consisting of a feed type (GTFS or NeTEx)
 * and a label, which is Route.type in GTFS and Trip.submode in NeTEx.
 */
public record SubmodeMappingMatcher(FeedType inputFeedType, String inputLabel) {}
