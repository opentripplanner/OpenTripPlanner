package org.opentripplanner.ext.fares.service.gtfs.v2;

import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.time.LocalDate;
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

  GtfsFaresV2Service(
    List<FareLegRule> legRules,
    List<FareTransferRule> fareTransferRules,
    Multimap<FeedScopedId, FeedScopedId> stopAreas,
    Multimap<FeedScopedId, LocalDate> serviceDatesForServiceId
  ) {
    this.lookup = new FareLookupService(
      legRules,
      fareTransferRules,
      stopAreas,
      serviceDatesForServiceId
    );
  }

  public static GtfsFaresV2ServiceBuilder of() {
    return new GtfsFaresV2ServiceBuilder();
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
          .forEach(tail -> {
            var unlimitedTransferOffers = lookup.findTransferOffersForSubLegs(
              split.head(),
              tail,
              FareTransferRule::unlimitedTransfers
            );
            tail.forEach(leg -> offerContainer.addToLeg(leg, unlimitedTransferOffers));
            var hasFreeTransfer = lookup.hasFreeTransfers(tail);
            if (hasFreeTransfer) {
              offerContainer.transferProducts(tail.getFirst(), tail);
            }

            var transfers = tail.size();
            var limitedTransferOffers = lookup.findTransferOffersForSubLegs(
              split.head(),
              tail,
              t -> t.limitedTransfers() && t.allowsNumberOfTransfers(transfers)
            );
            offerContainer.addTransferLimitedOffer(split.head(), tail, limitedTransferOffers);
          })
      );
    }

    var itinProducts = lookup.findTransfersMatchingAllLegs(
      transitLegs,
      FareTransferRule::unlimitedTransfers
    );
    return new FareResult(itinProducts, offerContainer.toMultimap());
  }

  /**
   * Returns false if this service contains any Fares V2 data, true otherwise.
   */
  public boolean isEmpty() {
    return lookup.isEmpty();
  }
}
