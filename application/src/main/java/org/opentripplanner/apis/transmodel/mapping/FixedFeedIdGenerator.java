package org.opentripplanner.apis.transmodel.mapping;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixedFeedIdGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(FixedFeedIdGenerator.class);

  public static String generate(Collection<? extends AbstractTransitEntity> entities) {
    String fixedFeedId = "UNKNOWN_FEED";

    // Count each feedId
    Map<String, Integer> feedIds = entities
      .stream()
      .map(a -> a.getId().getFeedId())
      .collect(Collectors.groupingBy(it -> it, Collectors.reducing(0, i -> 1, Integer::sum)));

    if (feedIds.isEmpty()) {
      LOG.warn("No data, unable to resolve fixedFeedScope to use in the Transmodel GraphQL API.");
    } else if (feedIds.size() == 1) {
      fixedFeedId = feedIds.keySet().iterator().next();
    } else {
      //noinspection OptionalGetWithoutIsPresent
      fixedFeedId = feedIds.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
      LOG.warn(
        "More than one feedId exist in the list of agencies. The feed-id used by" +
        "most agencies will be picked."
      );
    }
    LOG.info(
      "Starting Transmodel GraphQL Schema with fixed FeedId: '" +
      fixedFeedId +
      "'. All FeedScopedIds in API will be assumed to belong to this agency."
    );
    return fixedFeedId;
  }
}
