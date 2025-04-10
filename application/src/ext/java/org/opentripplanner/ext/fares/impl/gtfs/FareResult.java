package org.opentripplanner.ext.fares.impl.gtfs;

import java.util.Optional;
import java.util.Set;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Leg;

/**
 * @param itineraryProducts The fare products that cover the entire itinerary, like a daily pass.
 * @param legProducts       The fare products that cover only individual legs.
 */
record FareResult(Set<FareProduct> itineraryProducts, Set<FareProductMatch> legProducts) {
  public Optional<FareProductMatch> match(Leg leg) {
    return legProducts.stream().filter(lp -> lp.leg().equals(leg)).findFirst();
  }
}
