package org.opentripplanner.ext.fares.service.gtfs.v2;

import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Itinerary;
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
    var offerContainer = new LegOfferContainer();
    var transitLegs = itinerary.listTransitLegs();
    // individual legs
    transitLegs.forEach(leg -> {
      Set<LegOffer> legOffers = lookup
        .legRules(leg)
        .stream()
        .flatMap(r -> r.fareProducts().stream())
        .map(fp -> LegOffer.of(FareOffer.of(leg.startTime(), fp)))
        .collect(Collectors.toUnmodifiableSet());
      offerContainer.addToLeg(leg, legOffers);
    });

    // add transfers for subsections of the itinerary
    if (transitLegs.size() > 1) {
      var splits = ListUtils.partitionIntoSplits(transitLegs);
      splits.forEach(split ->
        split
          .subTails()
          .forEach(legs -> {
            var offers = lookup.findTransferOffersForSubLegs(split.head(), legs);
            legs.forEach(leg -> offerContainer.addToLeg(leg, offers));
            var hasFreeTransfer = lookup.hasFreeTransfers(legs);
            if (hasFreeTransfer) {
              offerContainer.transferProducts(legs.getFirst(), legs);
            }
          })
      );
    }

    var itinProducts = lookup.findTransfersMatchingAllLegs(transitLegs);
    return new FareResult(itinProducts, offerContainer.toMultimap());
  }

  /**
   * Returns false if this service contains any Fares V2 data, true otherwise.
   */
  public boolean isEmpty() {
    return lookup.isEmpty();
  }
}
