package org.opentripplanner.ext.fares.impl.gtfs;

import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
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

  public ProductResult calculateFareProducts(Itinerary itinerary) {
    var transitLegs = itinerary.listScheduledTransitLegs();

    var allLegProducts = new HashSet<FareProductMatch>();
    for (int i = 0; i < transitLegs.size(); i++) {
      var leg = transitLegs.get(i);
      var previousIndex = legAtIndex(i - 1, transitLegs);
      var nextLeg = legAtIndex(i + 1, transitLegs);

      var lp = getLegProduct(leg, previousIndex, nextLeg);
      allLegProducts.add(lp);
    }

    var coveringItinerary = productsCoveringItinerary(itinerary, allLegProducts);

    return new ProductResult(coveringItinerary, allLegProducts);
  }

  /**
   * Returns false if this services contains any Fares V2 data, true otherwise.
   */
  boolean isEmpty() {
    return lookup.isEmpty();
  }

  private static Optional<ScheduledTransitLeg> legAtIndex(
    int index,
    List<ScheduledTransitLeg> transitLegs
  ) {
    if (index >= 0 && index < transitLegs.size()) {
      return Optional.of(transitLegs.get(index));
    } else {
      return Optional.empty();
    }
  }

  private Set<FareProduct> productsCoveringItinerary(
    Itinerary itinerary,
    Collection<FareProductMatch> legProducts
  ) {
    var distinctProductWithTransferSets = legProducts
      .stream()
      .map(FareProductMatch::products)
      .collect(Collectors.toSet());

    return distinctProductWithTransferSets
      .stream()
      .flatMap(p -> p.stream().filter(ps -> coversItinerary(itinerary, ps)))
      .map(FareProductMatch.ProductWithTransfer::legRule)
      .flatMap(r -> r.fareProducts().stream())
      .collect(Collectors.toSet());
  }

  private boolean coversItinerary(Itinerary i, FareProductMatch.ProductWithTransfer pwt) {
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
    FareProductMatch.ProductWithTransfer pwt
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

  private FareProductMatch getLegProduct(
    ScheduledTransitLeg leg,
    Optional<ScheduledTransitLeg> previousLeg,
    Optional<ScheduledTransitLeg> nextLeg
  ) {
    var products = lookup.productsWithTransfer(leg, nextLeg);

    var transferFromPrevious = previousLeg
      .map(previous -> lookup.transfersFromPreviousLeg(previous, leg))
      .orElse(Set.of());

    return new FareProductMatch(leg, products, transferFromPrevious);
  }

  /**
   * @param itineraryProducts The fare products that cover the entire itinerary, like a daily pass.
   * @param legProducts       The fare products that cover only individual legs.
   */
  record ProductResult(Set<FareProduct> itineraryProducts, Set<FareProductMatch> legProducts) {
    public Optional<FareProductMatch> match(Leg leg) {
      return legProducts.stream().filter(lp -> lp.leg().equals(leg)).findFirst();
    }
  }
}
