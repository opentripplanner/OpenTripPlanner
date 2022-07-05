package org.opentripplanner.routing.core;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * <p>
 * FareComponent is a sequence of routes for a particular fare.
 * </p>
 */
public class FareComponent {

  public final FeedScopedId fareId;
  public final Money price;
  public final List<FeedScopedId> routes = new ArrayList<>();

  public FareComponent(FeedScopedId fareId, Money amount) {
    this.fareId = fareId;
    this.price = amount;
  }

  public void addRoute(FeedScopedId routeId) {
    routes.add(routeId);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addStr("id", fareId.toString())
      .addObj("price", price)
      .toString();
  }
}
