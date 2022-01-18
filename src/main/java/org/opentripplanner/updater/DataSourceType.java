package org.opentripplanner.updater;

public enum DataSourceType {
  // Vehicle Rental
  GBFS,
  SMOOVE,

  // Vehicle Parking
  KML,
  PARK_API,
  BICYCLE_PARK_API,
  HSL_PARK,

  // GTFS RT
  GTFS_RT_HTTP,
  GTFS_RT_FILE;
}
