package org.opentripplanner.ext.fares.impl.gtfs;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class NetworkMatcher {

  private final Set<FeedScopedId> networksWithRules;

  NetworkMatcher(List<FareLegRule> rules) {
    this.networksWithRules = findNetworksWithRules(rules);
  }

  /**
   * Get the fare products that match the network_id. If the network id of the product is null it
   * depends on the presence/absence of other rules with that network id.
   */
  boolean matchesNetworkId(TransitLeg leg, FareLegRule rule) {
    var routesNetworkIds = leg
      .route()
      .getGroupsOfRoutes()
      .stream()
      .map(AbstractTransitEntity::getId)
      .filter(Objects::nonNull)
      .toList();

    return (
      (rule.networkId() == null &&
        networksWithRules.stream().noneMatch(routesNetworkIds::contains)) ||
      routesNetworkIds.contains(rule.networkId())
    );
  }

  private static Set<FeedScopedId> findNetworksWithRules(Collection<FareLegRule> legRules) {
    return legRules
      .stream()
      .map(FareLegRule::networkId)
      .filter(Objects::nonNull)
      .collect(Collectors.toUnmodifiableSet());
  }
}
