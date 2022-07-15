package org.opentripplanner.routing.core;

import java.util.Currency;
import java.util.List;
import org.opentripplanner.model.FareContainer;
import org.opentripplanner.model.RiderCategory;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * <p>
 * FareComponent is a sequence of routes for a particular fare.
 * </p>
 */
public record FareComponent(
  FeedScopedId fareId,
  Money price,
  List<FeedScopedId> routes,
  FareContainer container,
  RiderCategory category
) {}
