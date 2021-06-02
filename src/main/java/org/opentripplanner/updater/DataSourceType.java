package org.opentripplanner.updater;

public enum DataSourceType {
  // Vehicle Rental
  GBFS,
  SMOOVE,

  // Vehicle Parking
  KML,

  // GTFS RT
  GTFS_RT_HTTP,
  GTFS_RT_FILE;
}
