package org.opentripplanner.datastore.api;

/**
 * Options that override the default validation applied when opening a remote GTFS data source.
 */
public record DataSourceOptions(boolean ignoreHttps, boolean ignoreZipExtension) {
  public static final DataSourceOptions DEFAULTS = new DataSourceOptions(false, false);
}