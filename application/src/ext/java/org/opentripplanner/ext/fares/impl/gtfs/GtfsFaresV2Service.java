package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.ListUtils;

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
    Multimap<Leg, FareOffer> legProducts = HashMultimap.create();
    var scheduledTransitLegs = itinerary.listScheduledTransitLegs();
    scheduledTransitLegs.forEach(leg -> {
      var products = lookup
        .legRules(leg)
        .stream()
        .flatMap(r -> r.fareProducts().stream())
        .map(FareOffer.DefaultFareOffer::new)
        .collect(Collectors.toUnmodifiableSet());
      legProducts.putAll(leg, products);
    });

    var pairs = ListUtils.partitionIntoOverlappingPairs(scheduledTransitLegs);
    pairs.forEach(pair ->
      lookup
        .findOffersForPair(pair.first(), pair.second())
        .forEach(transfer -> legProducts.put(pair.second(), transfer))
    );

    if (scheduledTransitLegs.size() > 1) {
      var splits = ListUtils.partitionIntoSplits(scheduledTransitLegs);
      splits.forEach(split ->
        split
          .tail()
          .forEach(leg -> {
            var offers = lookup.findOffersForPair(split.head(), leg);
            legProducts.putAll(leg, offers);
          })
      );
    }

    var itinProducts = lookup.findTransfersMatchingAllLegs(scheduledTransitLegs);
    return new FareResult(itinProducts, legProducts);
  }

  /**
   * Returns false if this service contains any Fares V2 data, true otherwise.
   */
  boolean isEmpty() {
    return lookup.isEmpty();
  }
}
