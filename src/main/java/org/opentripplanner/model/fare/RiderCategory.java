package org.opentripplanner.model.fare;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.transit.model.framework.FeedScopedId;

@Sandbox
public record RiderCategory(@Nonnull FeedScopedId id, @Nonnull String name, @Nullable String url) {
  public RiderCategory {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
  }
}
