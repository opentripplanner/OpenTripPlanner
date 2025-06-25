package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;

/**
 * A package-private helper class for making sure that only the "best" fare offers are added for
 * legs: it makes sure that free transfers are only added when there isn't already one for this
 * particular leg.
 */
class LegOfferContainer {

  private final SetMultimap<Leg, FareOffer> multimap = HashMultimap.create();

  /**
   * Take all offers from {@code from} and apply them to {@code targets}.
   */
  public void transferProducts(ScheduledTransitLeg from, List<ScheduledTransitLeg> targets) {
    var previousLegsProducts = multimap.get(from);
    targets.forEach(leg -> addToLeg(leg, previousLegsProducts));
  }

  void addToLeg(ScheduledTransitLeg leg, Collection<FareOffer> products) {
    products.forEach(p -> addToLeg(leg, p));
  }

  /**
   * Add an offer for a specific leg. If the given offer has a start time before a currently existing
   * one, this means that the currently existing one is worse than the proposed new one and
   * is removed.
   */
  void addToLeg(Leg leg, FareOffer offer) {
    var legContainsItAlready = multimap
      .get(leg)
      .stream()
      .filter(existingOffer -> existingOffer.fareProduct().equals(offer.fareProduct()))
      .findAny();

    if (legContainsItAlready.isPresent()) {
      var existing = legContainsItAlready.get();
      // there is a better offer with the same product available, remove the current one
      if (existing.startTime().isAfter(offer.startTime())) {
        multimap.remove(leg, existing);
        multimap.put(leg, offer);
      }
    } else {
      multimap.put(leg, offer);
    }
  }

  SetMultimap<Leg, FareOffer> toMultimap() {
    return ImmutableSetMultimap.copyOf(multimap);
  }
}
