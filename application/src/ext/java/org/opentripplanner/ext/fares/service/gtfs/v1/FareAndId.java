package org.opentripplanner.ext.fares.service.gtfs.v1;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.basic.Money;

/** Holds fare and corresponding fareId */
public record FareAndId(Money fare, FeedScopedId fareId) {}
