package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.model.framework.FeedScopedId;

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
    Multimap<Leg, FareProduct> legProducts = ArrayListMultimap.create();
    itinerary
      .listScheduledTransitLegs()
      .forEach(leg -> {
        var products = lookup
          .legRules(leg)
          .stream()
          .flatMap(r -> r.fareProducts().stream())
          .collect(Collectors.toUnmodifiableSet());
        legProducts.putAll(leg, products);
      });

    var legMatches = legProducts
      .asMap()
      .entrySet()
      .stream()
      .map(e -> new FareProductMatch(e.getKey(), Set.copyOf(e.getValue())))
      .collect(Collectors.toUnmodifiableSet());

    var itinProducts = lookup
      .transferRulesMatchingAllLegs(itinerary.listScheduledTransitLegs())
      .stream()
      .flatMap(transferRule ->
        Stream.concat(
          transferRule.fromLegRule().fareProducts().stream(),
          transferRule.toLegRule().fareProducts().stream()
        )
      )
      .collect(Collectors.toUnmodifiableSet());

    return new FareResult(itinProducts, legMatches);
  }

  /**
   * Returns false if this services contains any Fares V2 data, true otherwise.
   */
  boolean isEmpty() {
    return lookup.isEmpty();
  }
}
