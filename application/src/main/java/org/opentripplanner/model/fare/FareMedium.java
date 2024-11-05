package org.opentripplanner.model.fare;

import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;

/**
 *
 * The fare medium to "hold" a fare.
 * <p>
 * This can be cash, an app, smartcards, credit cards and others.
 */
@Sandbox
public record FareMedium(FeedScopedId id, String name) {
  public FareMedium {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
  }
}
