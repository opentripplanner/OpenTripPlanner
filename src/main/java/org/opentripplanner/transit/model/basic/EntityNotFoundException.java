package org.opentripplanner.transit.model.basic;

import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * When getting entities by Ids, throw this exception.
 */
public class EntityNotFoundException extends RuntimeException {

  public EntityNotFoundException(FeedScopedId id) {
    super("Entity not found. Id=" + id);
  }
}
