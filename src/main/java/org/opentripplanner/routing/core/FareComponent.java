package org.opentripplanner.routing.core;

import java.util.ArrayList;
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
public class FareComponent {

  public final FeedScopedId fareId;
  public final Money price;
  public final RiderCategory category;
  public final FareContainer container;
  public final List<FeedScopedId> routes = new ArrayList<>();

  public FareComponent(FeedScopedId fareId, Money amount) {
    this(fareId, amount, null, null);
  }

  public FareComponent(
    FeedScopedId fareId,
    Money amount,
    RiderCategory category,
    FareContainer container
  ) {
    this.fareId = fareId;
    this.price = amount;
    this.category = category;
    this.container = container;
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
      .addObj("category", category)
      .addObj("container", container)
      .toString();
  }
}
