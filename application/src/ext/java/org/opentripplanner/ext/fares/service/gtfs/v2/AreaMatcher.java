package org.opentripplanner.ext.fares.service.gtfs.v2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Matches the GTFS fares column from_area_id and to_area_id.
 */
class AreaMatcher {

  private final RulePriorityMatcher priorityMatcher;
  private final Multimap<FeedScopedId, FeedScopedId> stopAreas;
  private final Set<FeedScopedId> fromAreasWithRules;
  private final Set<FeedScopedId> toAreasWithRules;

  AreaMatcher(
    RulePriorityMatcher priorityMatcher,
    Collection<FareLegRule> rules,
    Multimap<FeedScopedId, FeedScopedId> stopAreas
  ) {
    this.priorityMatcher = priorityMatcher;
    this.stopAreas = ImmutableMultimap.copyOf(stopAreas);
    this.fromAreasWithRules = findAreasWithRules(rules, FareLegRule::fromAreaId);
    this.toAreasWithRules = findAreasWithRules(rules, FareLegRule::toAreaId);
  }

  boolean matchesFromArea(StopLocation stop, FeedScopedId areaId) {
    return matches(stop, areaId, fromAreasWithRules);
  }

  boolean matchesToArea(StopLocation stop, FeedScopedId areaId) {
    return matches(stop, areaId, toAreasWithRules);
  }

  private boolean matches(
    StopLocation stop,
    FeedScopedId areaId,
    Set<FeedScopedId> areasWithRules
  ) {
    var stopAreas = this.stopAreas.get(stop.getId());
    if (priorityMatcher.feedContainsRulePriority(stop.getId().getFeedId())) {
      return areaId == null || stopAreas.contains(areaId);
    } else {
      return (
        (isNull(areaId) && stopAreas.stream().noneMatch(areasWithRules::contains)) ||
        (nonNull(areaId) && stopAreas.contains(areaId))
      );
    }
  }

  private static Set<FeedScopedId> findAreasWithRules(
    Collection<FareLegRule> legRules,
    Function<FareLegRule, FeedScopedId> getArea
  ) {
    return legRules.stream().map(getArea).filter(Objects::nonNull).collect(Collectors.toSet());
  }
}
