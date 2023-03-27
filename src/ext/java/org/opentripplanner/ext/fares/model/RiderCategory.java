package org.opentripplanner.ext.fares.model;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record RiderCategory(FeedScopedId id, String name, @Nullable String url) {
  public RiderCategory {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
  }
}
