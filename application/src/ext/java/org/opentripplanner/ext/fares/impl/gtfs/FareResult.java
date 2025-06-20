package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Leg;

/**
 * @param itineraryProducts The fare products that cover the entire itinerary, like a daily pass.
 * @param legProducts       The fare products that cover only individual legs.
 */
record FareResult(Set<FareProduct> itineraryProducts, Multimap<Leg, FareOffer> legProducts) {
  public FareResult {
    legProducts = ImmutableSetMultimap.copyOf(legProducts);
  }
  public Collection<FareOffer> offersForLeg(Leg leg) {
    return legProducts.get(leg);
  }
}
