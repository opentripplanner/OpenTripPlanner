package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.grouppriority.TransitPriorityGroup32n;
import org.opentripplanner.routing.api.request.request.filter.TransitPriorityGroupSelect;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.RoutingTripPattern;

/**
 * This class dynamically build an index of transit-group-ids from the
 * provided {@link TransitPriorityGroupSelect}s while serving the caller with
 * group-ids for each requested pattern. It is made for optimal
 * performance, since it is used in request scope.
 * <p>
 * THIS CLASS IS NOT THREAD-SAFE.
 */
public class PriorityGroupConfigurator {

  private static final int BASE_GROUP_ID = TransitPriorityGroup32n.groupId(0);
  private int groupIndexCounter = 0;
  private final boolean enabled;
  private final PriorityGroupMatcher[] baseMatchers;
  private final PriorityGroupMatcher[] agencyMatchers;
  private final PriorityGroupMatcher[] globalMatchers;
  private final Map<PriorityGroupMatcher, Map<FeedScopedId, Integer>> agencyMatchersIds = new HashMap<>();
  private final Map<PriorityGroupMatcher, Integer> globalMatchersIds = new HashMap<>();

  private PriorityGroupConfigurator() {
    this.enabled = false;
    this.baseMatchers = null;
    this.agencyMatchers = null;
    this.globalMatchers = null;
  }

  private PriorityGroupConfigurator(
    Collection<TransitPriorityGroupSelect> base,
    Collection<TransitPriorityGroupSelect> byAgency,
    Collection<TransitPriorityGroupSelect> global
  ) {
    this.baseMatchers =
      base
        .stream()
        .map(PriorityGroupMatcher::of)
        .filter(Predicate.not(PriorityGroupMatcher::isEmpty))
        .toArray(PriorityGroupMatcher[]::new);
    this.agencyMatchers =
      byAgency
        .stream()
        .map(PriorityGroupMatcher::of)
        .filter(Predicate.not(PriorityGroupMatcher::isEmpty))
        .toArray(PriorityGroupMatcher[]::new);
    this.globalMatchers =
      global
        .stream()
        .map(PriorityGroupMatcher::of)
        .filter(Predicate.not(PriorityGroupMatcher::isEmpty))
        .toArray(PriorityGroupMatcher[]::new);
    this.enabled =
      (baseMatchers.length > 0) || (agencyMatchers.length > 0) || (globalMatchers.length > 0);
  }

  public static PriorityGroupConfigurator empty() {
    return new PriorityGroupConfigurator();
  }

  public static PriorityGroupConfigurator of(
    Collection<TransitPriorityGroupSelect> base,
    Collection<TransitPriorityGroupSelect> byAgency,
    Collection<TransitPriorityGroupSelect> global
  ) {
    if (Stream.of(base, byAgency, global).allMatch(Collection::isEmpty)) {
      return empty();
    }
    return new PriorityGroupConfigurator(base, byAgency, global);
  }

  /**
   * Fetch/lookup the transit-group-id for the given pattern.
   * <p>
   * @throws IllegalArgumentException if more than 32 group-ids are requested.
   */
  public int lookupTransitPriorityGroupId(RoutingTripPattern tripPattern) {
    if (!enabled) {
      return BASE_GROUP_ID;
    }

    var p = tripPattern.getPattern();

    for (PriorityGroupMatcher m : baseMatchers) {
      if (m.match(p)) {
        return BASE_GROUP_ID;
      }
    }
    for (var matcher : agencyMatchers) {
      if (matcher.match(p)) {
        var agencyIds = agencyMatchersIds.computeIfAbsent(matcher, m -> new HashMap<>());
        return agencyIds.computeIfAbsent(p.getRoute().getAgency().getId(), id -> nextGroupId());
      }
    }
    for (var matcher : globalMatchers) {
      if (matcher.match(p)) {
        return globalMatchersIds.computeIfAbsent(matcher, it -> nextGroupId());
      }
    }
    // Fallback to base-group-id
    return BASE_GROUP_ID;
  }

  private int nextGroupId() {
    return TransitPriorityGroup32n.groupId(++groupIndexCounter);
  }
}
