package org.opentripplanner.transit.framework;

import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * An object in the transit model with an identity. The id is unique within the context the entity
 * lives in, for example among all stops.
 */
public interface TransitEntity {
  FeedScopedId getId();
}
