package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
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
    Multimap<Leg, TransferFareProduct> legProducts = HashMultimap.create();
    itinerary
      .listScheduledTransitLegs()
      .forEach(leg -> {
        var products = lookup
          .legRules(leg)
          .stream()
          .flatMap(r -> r.fareProducts().stream())
          .map(TransferFareProduct::new)
          .collect(Collectors.toUnmodifiableSet());
        legProducts.putAll(leg, products);
      });

    var pairs = ListUtils.partitionIntoOverlappingPairs(itinerary.listScheduledTransitLegs());
    pairs.forEach(pair -> {
      final Set<TransferFareProduct> transferMatches = lookup.findTransfersForPair(
        pair.first(),
        pair.second()
      );
      transferMatches.forEach(transfer -> {
        legProducts.put(pair.second(), transfer);
      });
    });
    var itinProducts = lookup
      .transfersMatchingAllLegs(itinerary.listScheduledTransitLegs())
      .stream()
      .flatMap(transferRule ->
        ListUtils.combine(
          transferRule.fromLegRule().fareProducts(),
          transferRule.toLegRule().fareProducts(),
          transferRule.transferRule().fareProducts()
        ).stream()
      )
      .collect(Collectors.toUnmodifiableSet());

    return new FareResult(itinProducts, legProducts);
  }

  /**
   * Returns false if this services contains any Fares V2 data, true otherwise.
   */
  boolean isEmpty() {
    return lookup.isEmpty();
  }
}
