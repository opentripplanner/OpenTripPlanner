package org.opentripplanner.routing.core;

import java.util.List;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * <p>
 * FareComponent is a sequence of routes for a particular fare.
 * </p>
 */
@Deprecated
public record FareComponent(FeedScopedId fareId, String name, Money price, List<Leg> legs) {
  public List<FeedScopedId> routes() {
    return legs.stream().map(l -> l.getRoute().getId()).toList();
  }
}
