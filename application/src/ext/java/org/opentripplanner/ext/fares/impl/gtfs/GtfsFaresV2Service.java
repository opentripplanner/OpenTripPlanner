package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.ListUtils;

/**
 * Computes fare offers derived from GTFS Fares V2 data.
 */
public final class GtfsFaresV2Service implements Serializable {

  private final FareLookupService lookup;

  public GtfsFaresV2Service(
    List<FareLegRule> legRules,
    List<FareTransferRule> fareTransferRules,
    Multimap<FeedScopedId, FeedScopedId> stopAreas
  ) {
    this.lookup = new FareLookupService(legRules, fareTransferRules, stopAreas);
  }

  public FareResult calculateFares(Itinerary itinerary) {
    var legOffers = new LegOfferContainer();
    var scheduledTransitLegs = itinerary.listScheduledTransitLegs();
    // individual legs
    scheduledTransitLegs.forEach(leg -> {
      var products = lookup
        .legRules(leg)
        .stream()
        .flatMap(r -> r.fareProducts().stream())
        .map(fp -> FareOffer.of(leg.startTime(), fp))
        .collect(Collectors.toUnmodifiableSet());
      legOffers.addToLeg(leg, products);
    });

    // add transfers for subsections of the itinerary
    if (scheduledTransitLegs.size() > 1) {
      var splits = ListUtils.partitionIntoSplits(scheduledTransitLegs);
      splits.forEach(split ->
        split
          .subTails()
          .forEach(legs -> {
            var offers = lookup.findTransferOffersForSubLegs(split.head(), legs);
            legs.forEach(leg -> legOffers.addToLeg(leg, offers));
            var hasFreeTransfer = lookup.hasFreeTransfer(split.head(), legs);
            if (hasFreeTransfer) {
              legOffers.transferProducts(split.head(), legs);
            }
          })
      );
    }

    var itinProducts = lookup.findTransfersMatchingAllLegs(scheduledTransitLegs);
    return new FareResult(itinProducts, legOffers.toMultimap());
  }

  /**
   * Returns false if this service contains any Fares V2 data, true otherwise.
   */
  boolean isEmpty() {
    return lookup.isEmpty();
  }
}
