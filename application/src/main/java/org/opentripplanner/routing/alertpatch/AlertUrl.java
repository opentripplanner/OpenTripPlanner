package org.opentripplanner.routing.alertpatch;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record AlertUrl(String uri, @Nullable String label) {
  public AlertUrl {
    Objects.requireNonNull(uri);
  }
}
