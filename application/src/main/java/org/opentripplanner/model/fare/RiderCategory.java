package org.opentripplanner.model.fare;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;

@Sandbox
public record RiderCategory(FeedScopedId id, String name, @Nullable String url) {
  public RiderCategory {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
  }
}
