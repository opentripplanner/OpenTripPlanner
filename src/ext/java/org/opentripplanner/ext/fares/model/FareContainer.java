package org.opentripplanner.ext.fares.model;

import org.opentripplanner.transit.model.framework.FeedScopedId;

public record FareContainer(FeedScopedId id, String name) {}
