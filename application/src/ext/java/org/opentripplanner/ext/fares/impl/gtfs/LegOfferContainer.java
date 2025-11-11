package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.TransitLeg;

/**
 * A package-private helper class for making sure that only the "best" fare offers are added for
 * legs: it makes sure that free transfers are only added when there isn't already one for this
 * particular leg.
 */
class LegOfferContainer {

  private final SetMultimap<Leg, LegOffer> multimap = HashMultimap.create();

  /**
   * Take all offers from {@code from} and apply them to {@code targets}.
   */
  public void transferProducts(TransitLeg from, List<TransitLeg> targets) {
    var previousLegsOffer = multimap.get(from);
    previousLegsOffer.forEach(previousOffer -> {
      if (previousOffer.withinTimeLimit(targets.getLast())) {
        targets.forEach(leg -> addToLeg(leg, previousOffer));
      }
    });
  }

  void addToLeg(TransitLeg leg, Collection<LegOffer> products) {
    products.forEach(p -> addToLeg(leg, p));
  }

  /**
   * Add an offer for a specific leg. If the given offer has a start time before a currently existing
   * one, this means that the currently existing one is worse than the proposed new one and
   * is removed.
   */
  void addToLeg(Leg leg, LegOffer legOffer) {
    var legContainsItAlready = multimap
      .get(leg)
      .stream()
      .filter(existingOffer -> existingOffer.fareProduct().equals(legOffer.fareProduct()))
      .findAny();

    if (legContainsItAlready.isPresent()) {
      var existing = legContainsItAlready.get();
      // there is a better offer with the same product available, remove the current one
      if (existing.offer().startTime().isAfter(legOffer.offer().startTime())) {
        multimap.remove(leg, existing);
        multimap.put(leg, legOffer);
      }
    } else {
      multimap.put(leg, legOffer);
    }
  }

  SetMultimap<Leg, FareOffer> toMultimap() {
    var ret = ImmutableSetMultimap.<Leg, FareOffer>builder();
    multimap.forEach((leg, offer) -> ret.put(leg, offer.offer()));
    return ret.build();
  }
}
