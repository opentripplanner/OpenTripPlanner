package org.opentripplanner.ext.fares.model;

import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * The fare medium to "hold" a fare.
 * <p>
 * This can be cash, an app, smartcards, credit cards and others.
 */
public record FareMedium(FeedScopedId id, String name) {
  public FareMedium {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
  }
}
