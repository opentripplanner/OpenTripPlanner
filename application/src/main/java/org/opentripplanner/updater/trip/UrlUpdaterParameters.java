package org.opentripplanner.updater.trip;

public interface UrlUpdaterParameters {
  String url();
  String configRef();
  String feedId();

  default boolean detailedMetrics() {
    return false;
  }
}
