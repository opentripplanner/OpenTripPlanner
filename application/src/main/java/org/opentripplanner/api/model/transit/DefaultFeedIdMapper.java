package org.opentripplanner.api.model.transit;

import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * An id mapper that passes input and output ids through with the feed id.
 */
public class DefaultFeedIdMapper implements FeedScopedIdMapper {

  @Override
  public Optional<FeedScopedId> parse(String id) {
    return FeedScopedId.parseOptional(id);
  }

  @Override
  public String mapToApi(FeedScopedId id) {
    return id.toString();
  }
}
