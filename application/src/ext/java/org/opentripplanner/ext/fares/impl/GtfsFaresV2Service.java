package org.opentripplanner.ext.fares.impl;

import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.impl.gtfs.FareLookupService;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.model.LegProducts;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
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

  public ProductResult getProducts(Itinerary itinerary) {
    var transitLegs = itinerary.listScheduledTransitLegs();

    var allLegProducts = new HashSet<LegProducts>();
    for (int i = 0; i < transitLegs.size(); i++) {
      var leg = transitLegs.get(i);
      var nextIndex = i + 1;

      Optional<ScheduledTransitLeg> nextLeg = Optional.empty();
      if (nextIndex < transitLegs.size()) {
        nextLeg = Optional.of(transitLegs.get(nextIndex));
      }

      var lp = getLegProduct(leg, nextLeg);
      allLegProducts.add(lp);
    }

    var coveringItinerary = productsCoveringItinerary(itinerary, allLegProducts);

    return new ProductResult(coveringItinerary, allLegProducts);
  }

  private Set<FareProduct> productsCoveringItinerary(
    Itinerary itinerary,
    Collection<LegProducts> legProducts
  ) {
    var distinctProductWithTransferSets = legProducts
      .stream()
      .map(LegProducts::products)
      .collect(Collectors.toSet());

    return distinctProductWithTransferSets
      .stream()
      .flatMap(p -> p.stream().filter(ps -> coversItinerary(itinerary, ps)))
      .map(LegProducts.ProductWithTransfer::legRule)
      .flatMap(r -> r.fareProducts().stream())
      .collect(Collectors.toSet());
  }

  private boolean coversItinerary(Itinerary i, LegProducts.ProductWithTransfer pwt) {
    var transitLegs = i.listScheduledTransitLegs();
    var allLegsInProductFeed = transitLegs
      .stream()
      .allMatch(leg -> leg.getAgency().getId().getFeedId().equals(pwt.legRule().feedId()));

    return (
      allLegsInProductFeed &&
      (transitLegs.size() == 1 ||
        (pwt.products().stream().anyMatch(p -> p.coversDuration(i.totalTransitDuration())) &&
          appliesToAllLegs(pwt.legRule(), transitLegs)) ||
        coversItineraryWithFreeTransfers(i, pwt))
    );
  }

  private boolean appliesToAllLegs(FareLegRule legRule, List<ScheduledTransitLeg> transitLegs) {
    return transitLegs.stream().allMatch(leg -> lookup.legMatchesRule(leg, legRule));
  }

  private boolean coversItineraryWithFreeTransfers(
    Itinerary i,
    LegProducts.ProductWithTransfer pwt
  ) {
    var feedIdsInItinerary = i
      .listScheduledTransitLegs()
      .stream()
      .map(l -> l.getAgency().getId().getFeedId())
      .collect(Collectors.toSet());

    return (
      feedIdsInItinerary.size() == 1 &&
      pwt.transferRules().stream().anyMatch(FareTransferRule::isFree)
    );
  }

  private LegProducts getLegProduct(
    ScheduledTransitLeg leg,
    Optional<ScheduledTransitLeg> nextLeg
  ) {
    var legRules = lookup.legRules(leg);

    final var products = lookup.getProductWithTransfers(leg, nextLeg, legRules);

    return new LegProducts(leg, nextLeg, products);
  }

  /**
   * @param itineraryProducts The fare products that cover the entire itinerary, like a daily pass.
   * @param legProducts       The fare products that cover only individual legs.
   */
  record ProductResult(Set<FareProduct> itineraryProducts, Set<LegProducts> legProducts) {
    public Set<FareProduct> getProducts(Leg leg) {
      return legProducts
        .stream()
        .filter(lp -> lp.leg().equals(leg))
        .findFirst()
        .map(l ->
          l.products().stream().flatMap(lp -> lp.products().stream()).collect(Collectors.toSet())
        )
        .orElse(Set.of());
    }
  }
}
