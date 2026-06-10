package org.opentripplanner.ext.fares.service.gtfs.v2;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Leg;

/**
 * @param legProducts       The fare products that cover only individual legs.
 */
public record FareResult(Multimap<Leg, FareOffer> legProducts) {
  public FareResult {
    legProducts = ImmutableSetMultimap.copyOf(legProducts);
  }

  public Collection<FareOffer> offersForLeg(Leg leg) {
    return legProducts.get(leg);
  }
}
