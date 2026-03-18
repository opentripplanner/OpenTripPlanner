package org.opentripplanner.ext.fares.service.gtfs.v2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.TransitLeg;

/**
 * A package-private helper class for making sure that only the "best" fare offers are added for
 * legs: it makes sure that free transfers are only added when there isn't already one for this
 * particular leg.
 */
class LegOfferContainer {

  private record TransferLimitedOffer(TransitLeg head, List<TransitLeg> tail, LegOffer legOffer) {}

  private final SetMultimap<Leg, LegOffer> unlimitedTransferOffers = HashMultimap.create();

  /**
   * Offers which are limited by the number of transfers. These need special handling.
   * See {@link LegOfferContainer#addTransferLimitedOffer(TransitLeg, List, Collection)}.
   */
  private final List<TransferLimitedOffer> transferLimitedOffers = new ArrayList<>();

  /**
   * Take all offers from {@code from} and apply them to {@code targets}.
   */
  void transferProducts(TransitLeg from, List<TransitLeg> targets) {
    var previousLegOffer = unlimitedTransferOffers.get(from);
    previousLegOffer.forEach(previousOffer -> {
      if (previousOffer.withinTimeLimit(targets.getLast())) {
        targets.forEach(leg -> addToLeg(leg, previousOffer));
      }
    });
  }

  /**
   * Add a leg offer that is contstrained by the number of transfers. This means they cannot be
   * transferred from one leg to another with
   * {@link LegOfferContainer#transferProducts(TransitLeg, List)}.
   */
  void addTransferLimitedOffer(
    TransitLeg head,
    List<TransitLeg> tail,
    Collection<LegOffer> limitedTransferOffers
  ) {
    limitedTransferOffers
      .stream()
      .filter(o -> containsNoTransferLimitedOffersAlready(head, o))
      .forEach(o -> transferLimitedOffers.add(new TransferLimitedOffer(head, tail, o)));
  }

  void addToLeg(TransitLeg leg, Collection<LegOffer> products) {
    products.forEach(p -> addToLeg(leg, p));
  }

  /**
   * Add an offer for a specific leg. If the given offer has a start time before a currently existing
   * one, this means that the currently existing one is worse than the proposed new one and
   * is removed.
   */
  private void addToLeg(TransitLeg leg, LegOffer legOffer) {
    var legContainsItAlready = unlimitedTransferOffers
      .get(leg)
      .stream()
      .filter(existingOffer -> existingOffer.fareProduct().equals(legOffer.fareProduct()))
      .findAny();

    if (legContainsItAlready.isPresent()) {
      var existing = legContainsItAlready.get();

      // there is a better offer with the same product available, remove the current one
      if (existing.offer().startTime().isAfter(legOffer.offer().startTime())) {
        unlimitedTransferOffers.remove(leg, existing);
        unlimitedTransferOffers.put(leg, legOffer);
      }
    } else {
      unlimitedTransferOffers.put(leg, legOffer);
    }
  }

  SetMultimap<Leg, FareOffer> toMultimap() {
    transferLimitedOffers.forEach(t -> {
      t.tail().forEach(l -> addToLeg(l, t.legOffer));
    });

    var ret = ImmutableSetMultimap.<Leg, FareOffer>builder();
    unlimitedTransferOffers.forEach((leg, offer) -> ret.put(leg, offer.offer()));

    return ret.build();
  }

  @Override
  public String toString() {
    return unlimitedTransferOffers
      .keys()
      .stream()
      .sorted(Comparator.comparing(Leg::startTime))
      .map(l -> l.startTime().toString() + " -> " + unlimitedTransferOffers.get(l))
      .collect(Collectors.joining(",", "{", "}"));
  }

  private boolean containsNoTransferLimitedOffersAlready(TransitLeg head, LegOffer o) {
    return transferLimitedOffers
      .stream()
      .noneMatch(
        t ->
          t.legOffer.offer().fareProduct().equals(o.offer().fareProduct()) && t.tail.contains(head)
      );
  }
}
