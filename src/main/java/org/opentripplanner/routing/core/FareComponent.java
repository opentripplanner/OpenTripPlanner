package org.opentripplanner.routing.core;

import java.util.List;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * <p>
 * FareComponent is a sequence of routes for a particular fare.
 * </p>
 * @deprecated Because it exists only for backwards compatibility, and you should use the Fares V2
 * type, namely {@link org.opentripplanner.model.fare.FareProduct}.
 */
@Deprecated
public record FareComponent(FeedScopedId fareId, Money price, List<Leg> legs) {
  public List<FeedScopedId> routes() {
    return legs.stream().map(l -> l.getRoute().getId()).toList();
  }
}
