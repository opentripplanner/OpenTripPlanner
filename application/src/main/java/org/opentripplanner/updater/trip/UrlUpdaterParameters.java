package org.opentripplanner.updater.trip;

import javax.annotation.Nullable;

public interface UrlUpdaterParameters {
  @Nullable
  String url();

  String configRef();
  String feedId();

  default boolean producerMetrics() {
    return false;
  }
}
