package org.opentripplanner.apis.transmodel.mapping;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitIdMapper {

  private static final Logger LOG = LoggerFactory.getLogger(TransitIdMapper.class);

  private static String fixedFeedId = null;

  public static String mapEntityIDToApi(AbstractTransitEntity entity) {
    if (entity == null) {
      return null;
    }

    return mapIDToApi(entity.getId());
  }

  public static String mapIDToApi(FeedScopedId id) {
    if (fixedFeedId != null) {
      return id.getId();
    }
    // It is safe to use the toString(), se the JavaDoc on it
    return id.toString();
  }

  /**
   * Maps ids to feed-scoped ids.
   * Return an empty collection if the collection of ids is null.
   * If the collection of ids contains null or blank elements, they are ignored.
   */
  public static List<FeedScopedId> mapIDsToDomainNullSafe(@Nullable Collection<String> ids) {
    if (ids == null) {
      return List.of();
    }
    return ids.stream().filter(StringUtils::hasValue).map(TransitIdMapper::mapIDToDomain).toList();
  }

  /**
   * Maps ids to feed-scoped ids.
   * Return null if the collection of ids is null.
   * If the collection of ids contains null or blank elements, they are ignored.
   */
  public static List<FeedScopedId> mapIDsToDomain(@Nullable Collection<String> ids) {
    if (ids == null) {
      return null;
    }
    return ids.stream().filter(StringUtils::hasValue).map(TransitIdMapper::mapIDToDomain).toList();
  }

  public static FeedScopedId mapIDToDomain(String id) {
    if (id == null || id.isBlank()) {
      return null;
    }
    if (fixedFeedId != null) {
      return new FeedScopedId(fixedFeedId, id);
    }
    return FeedScopedId.parse(id);
  }

  /**
   * This initialize the 'fixedFeedId', before this is done the GraphQL API will use the full id
   * including the feedId.
   * <p>
   * THIS CODE IS NOT THREAD SAFE!
   * <p>
   * Make sure to call this method BEFORE the server is stating-up/handling client requests.
   *
   * @param entities The entities to pick the feedId from, if more than one feedID exist, the feedId
   *                 with the most occurrences will be used. This is done to prevent a few "cases"
   *                 of wrongly set feedIds to block the entire API from working.
   * @return the fixedFeedId - used to unit test this method.
   */
  public static String setupFixedFeedId(Collection<? extends AbstractTransitEntity> entities) {
    fixedFeedId = "UNKNOWN_FEED";

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

  /**
   * Clear the globally configured feed id.
   * <p>
   * For use from tests only.
   *
   * @see #setupFixedFeedId(Collection)
   */
  public static void clearFixedFeedId() {
    fixedFeedId = null;
  }
}
