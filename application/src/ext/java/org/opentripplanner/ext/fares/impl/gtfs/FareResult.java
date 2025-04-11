package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Leg;

/**
 * @param itineraryProducts The fare products that cover the entire itinerary, like a daily pass.
 * @param legProducts       The fare products that cover only individual legs.
 */
record FareResult(Set<FareProduct> itineraryProducts, SetMultimap<Leg, FareProduct> legProducts) {
  public Collection<FareProduct> productsForLeg(Leg leg) {
    return legProducts.get(leg);
  }
}
