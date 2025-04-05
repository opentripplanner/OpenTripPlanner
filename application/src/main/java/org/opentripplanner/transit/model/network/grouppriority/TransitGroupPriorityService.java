package org.opentripplanner.transit.model.network.grouppriority;

import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.request.filter.TransitGroupSelect;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.lang.ArrayUtils;

/**
 * This class dynamically builds an index of transit-group-ids from the provided
 * {@link TransitGroupSelect}s while serving the caller with group-ids for each requested
 * trip/pattern. It is made for optimal performance, since it is used in request scope.
 * <p>
 * THIS CLASS IS NOT THREAD-SAFE.
 */
public class TransitGroupPriorityService {

  /**
   * IMPLEMENTATION DETAILS
   *
   * There are two ways we can treat the base (local-traffic) transit priority group:
   * <ol>
   *   <li>
   *     We can assign group id 1 (one) to the base group and it will be treated as any other group.
   *   </li>
   *   <li>
   *     We can assign group id 0 (zero) to the base and it will not be added to the set of groups
   *     a given path has.
   *   </li>
   * </ol>
   * When we compare paths, we compare sets of group ids. A set is dominating another set if it is
   * a smaller subset or different from the other set.
   */
  private static final int GROUP_INDEX_COUNTER_START = 1;

  private final int baseGroupId = TransitGroupPriority32n.groupId(GROUP_INDEX_COUNTER_START);
  private int groupIndexCounter = GROUP_INDEX_COUNTER_START;
  private final boolean enabled;
  private final Matcher[] agencyMatchers;
  private final Matcher[] globalMatchers;

  // Index matchers and ids
  private final List<MatcherAgencyAndIds> agencyMatchersIds;
  private final List<MatcherAndId> globalMatchersIds;

  private TransitGroupPriorityService() {
    this.enabled = false;
    this.agencyMatchers = null;
    this.globalMatchers = null;
    this.agencyMatchersIds = List.of();
    this.globalMatchersIds = List.of();
  }

  public TransitGroupPriorityService(
    Collection<TransitGroupSelect> byAgency,
    Collection<TransitGroupSelect> global
  ) {
    this.agencyMatchers = Matchers.of(byAgency);
    this.globalMatchers = Matchers.of(global);
    this.enabled = Stream.of(agencyMatchers, globalMatchers).anyMatch(ArrayUtils::hasContent);
    this.globalMatchersIds = Arrays.stream(globalMatchers)
      .map(m -> new MatcherAndId(m, nextGroupId()))
      .toList();
    // We need to populate this dynamically
    this.agencyMatchersIds = Arrays.stream(agencyMatchers).map(MatcherAgencyAndIds::new).toList();
  }

  public static TransitGroupPriorityService empty() {
    return new TransitGroupPriorityService();
  }

  public static TransitGroupPriorityService of(
    CostLinearFunction relaxTransitGroupPriority,
    List<TransitGroupSelect> groupByAgency,
    List<TransitGroupSelect> groupGlobal
  ) {
    if (relaxTransitGroupPriority.isNormal()) {
      return TransitGroupPriorityService.empty();
    } else if (Stream.of(groupByAgency, groupGlobal).allMatch(Collection::isEmpty)) {
      return TransitGroupPriorityService.empty();
    } else {
      return new TransitGroupPriorityService(groupByAgency, groupGlobal);
    }
  }

  /**
   * Return true is the feature is configured and the request a {@code relaxTransitGroupPriority}
   * function.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Fetch/lookup the transit-group-id for the given pattern.
   * <p>
   * @throws IllegalArgumentException if more than 32 group-ids are requested.
   */
  public int lookupTransitGroupPriorityId(TripPattern tripPattern) {
    return tripPattern == null
      ? baseGroupId
      : lookupTransitGroupPriorityId(new TripPatternAdapter(tripPattern));
  }

  /**
   * Fetch/lookup the transit-group-id for the given trip.
   * <p>
   * @throws IllegalArgumentException if more than 32 group-ids are requested.
   */
  public int lookupTransitGroupPriorityId(Trip trip) {
    return trip == null ? baseGroupId : lookupTransitGroupPriorityId(new TripAdapter(trip));
  }

  /**
   * Fetch/lookup the transit-group-id for the given entity.
   * <p>
   * @throws IllegalArgumentException if more than 32 group-ids are requested.
   */
  private int lookupTransitGroupPriorityId(EntityAdapter entity) {
    if (!enabled) {
      return baseGroupId;
    }
    for (var it : agencyMatchersIds) {
      if (it.matcher().match(entity)) {
        var agencyId = entity.agencyId();
        int groupId = it.ids().get(agencyId);

        if (groupId < 0) {
          groupId = nextGroupId();
          it.ids.put(agencyId, groupId);
        }
        return groupId;
      }
    }

    for (var it : globalMatchersIds) {
      if (it.matcher.match(entity)) {
        return it.groupId();
      }
    }
    // Fallback to base-group-id
    return baseGroupId;
  }

  /**
   * This is the group-id assigned to all transit trips/patterns witch does not match a
   * specific group.
   */
  public int baseGroupId() {
    return baseGroupId;
  }

  private int nextGroupId() {
    return TransitGroupPriority32n.groupId(++groupIndexCounter);
  }

  /** Pair of matcher and groupId. Used only inside this class. */
  private record MatcherAndId(Matcher matcher, int groupId) {}

  /** Matcher with a map of ids by agency. */
  private record MatcherAgencyAndIds(Matcher matcher, TObjectIntMap<FeedScopedId> ids) {
    MatcherAgencyAndIds(Matcher matcher) {
      this(
        matcher,
        new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1)
      );
    }
  }
}
