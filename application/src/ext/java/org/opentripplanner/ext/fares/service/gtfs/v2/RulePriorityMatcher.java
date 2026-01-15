package org.opentripplanner.ext.fares.service.gtfs.v2;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareLegRule;

/**
 * The column rule_priority changes the matching of the fields network_id, from_area_id and
 * to_area_id.
 * <p>
 * If a feed contains the column rule_priority, then a missing value doesn't mean "match all" but
 * rather "ignore this field".
 */
class RulePriorityMatcher {

  private final Set<String> feedsWithPriorities;

  RulePriorityMatcher(Collection<FareLegRule> rules) {
    this.feedsWithPriorities = rules
      .stream()
      .filter(r -> r.priority().isPresent())
      .map(FareLegRule::feedId)
      .collect(Collectors.toUnmodifiableSet());
  }

  boolean feedContainsRulePriority(String feedId) {
    return feedsWithPriorities.contains(feedId);
  }
}
