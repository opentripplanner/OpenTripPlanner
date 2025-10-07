package org.opentripplanner.ext.fares.impl.gtfs;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

class AreaMatcher {

  private final Multimap<FeedScopedId, FeedScopedId> stopAreas;
  private final Set<String> feedsWithPriorities;

  AreaMatcher(Collection<FareLegRule> rules, Multimap<FeedScopedId, FeedScopedId> stopAreas) {
    this.stopAreas = ImmutableMultimap.copyOf(stopAreas);
    this.feedsWithPriorities = rules
      .stream()
      .filter(r -> r.priority().isPresent())
      .map(FareLegRule::feedId)
      .collect(Collectors.toUnmodifiableSet());
  }

  boolean matchesArea(StopLocation stop, FeedScopedId areaId, Set<FeedScopedId> areasWithRules) {
    var stopAreas = this.stopAreas.get(stop.getId());
    if (feedsWithPriorities.contains(stop.getId().getFeedId())) {
      return areaId == null || stopAreas.contains(areaId);
    } else {
      return (
        (isNull(areaId) && stopAreas.stream().noneMatch(areasWithRules::contains)) ||
        (nonNull(areaId) && stopAreas.contains(areaId))
      );
    }
  }
}
