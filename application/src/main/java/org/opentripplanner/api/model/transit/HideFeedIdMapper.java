package org.opentripplanner.api.model.transit;

import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * An id mapper that appends a configured feed id to all input ids and strips the
 * feed id from all output ids.
 * This only works if your deployment only contains a single feed id.
 */
public class HideFeedIdMapper implements FeedScopedIdMapper {

  private final String feedId;

  public HideFeedIdMapper(String feedId) {
    this.feedId = Objects.requireNonNull(feedId);
  }

  @Override
  public Optional<FeedScopedId> parse(String id) {
    return FeedScopedId.ofOptional(feedId, id);
  }

  @Override
  public String mapToApi(FeedScopedId id) {
    return id.getId();
  }
}
