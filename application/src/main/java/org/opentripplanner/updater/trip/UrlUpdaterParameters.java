package org.opentripplanner.updater.trip;

import org.jspecify.annotations.Nullable;

public interface UrlUpdaterParameters {
  @Nullable
  String url();

  String configRef();
  String feedId();

  default boolean producerMetrics() {
    return false;
  }
}
