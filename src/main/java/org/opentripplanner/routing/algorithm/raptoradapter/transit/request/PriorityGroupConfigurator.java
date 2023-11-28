package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.framework.lang.ArrayUtils;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.grouppriority.TransitPriorityGroup32n;
import org.opentripplanner.routing.api.request.request.filter.TransitPriorityGroupSelect;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.RoutingTripPattern;

/**
 * This class dynamically builds an index of transit-group-ids from the
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

  // Index matchers and ids
  private final List<MatcherAgencyAndIds> agencyMatchersIds;
  private final List<MatcherAndId> globalMatchersIds;

  private PriorityGroupConfigurator() {
    this.enabled = false;
    this.baseMatchers = null;
    this.agencyMatchers = null;
    this.globalMatchers = null;
    this.agencyMatchersIds = List.of();
    this.globalMatchersIds = List.of();
  }

  private PriorityGroupConfigurator(
    Collection<TransitPriorityGroupSelect> base,
    Collection<TransitPriorityGroupSelect> byAgency,
    Collection<TransitPriorityGroupSelect> global
  ) {
    this.baseMatchers = PriorityGroupMatcher.of(base);
    this.agencyMatchers = PriorityGroupMatcher.of(byAgency);
    this.globalMatchers = PriorityGroupMatcher.of(global);
    this.enabled =
      Stream.of(baseMatchers, agencyMatchers, globalMatchers).anyMatch(ArrayUtils::hasContent);
    this.globalMatchersIds =
      Arrays.stream(globalMatchers).map(m -> new MatcherAndId(m, nextGroupId())).toList();
    // We need to populate this dynamically
    this.agencyMatchersIds = Arrays.stream(agencyMatchers).map(MatcherAgencyAndIds::new).toList();
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
    if (!enabled || tripPattern == null) {
      return BASE_GROUP_ID;
    }

    var p = tripPattern.getPattern();

    for (PriorityGroupMatcher m : baseMatchers) {
      if (m.match(p)) {
        return BASE_GROUP_ID;
      }
    }
    for (var it : agencyMatchersIds) {
      if (it.matcher().match(p)) {
        var agencyId = p.getRoute().getAgency().getId();
        int groupId = it.ids().get(agencyId);

        if (groupId < 0) {
          groupId = nextGroupId();
          it.ids.put(agencyId, groupId);
        }
        return groupId;
      }
    }

    for (var it : globalMatchersIds) {
      if (it.matcher.match(p)) {
        return it.groupId();
      }
    }
    // Fallback to base-group-id
    return BASE_GROUP_ID;
  }

  private int nextGroupId() {
    return TransitPriorityGroup32n.groupId(++groupIndexCounter);
  }

  /** Pair of matcher and groupId. Used only inside this class. */
  record MatcherAndId(PriorityGroupMatcher matcher, int groupId) {}

  /** Matcher with map of ids by agency. */
  record MatcherAgencyAndIds(PriorityGroupMatcher matcher, TObjectIntMap<FeedScopedId> ids) {
    MatcherAgencyAndIds(PriorityGroupMatcher matcher) {
      this(
        matcher,
        new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1)
      );
    }
  }
}
