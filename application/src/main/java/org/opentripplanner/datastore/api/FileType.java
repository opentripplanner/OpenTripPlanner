package org.opentripplanner.datastore.api;

import java.util.EnumSet;

/**
 * Represents the different types of files that might be present in a router / graph build
 * directory. We want to detect even those that are not graph builder inputs so we can effectively
 * warn when unknown file types are present. This helps point out when config files have been
 * misnamed (builder-config vs. build-config).
 */
public enum FileType {
  CONFIG("⚙️", "Config file"),
  OSM("🌍", "OpenStreetMap data"),
  DEM("🏔", "Elevation data"),
  GTFS("🚌", "GTFS data"),
  NETEX("🚌", "NeTEx data"),
  EMISSION("🌿", "Emission data"),
  EMPIRICAL_DATA("📊", "Emperical data"),
  GRAPH("🌐", "OTP Graph file"),
  REPORT("📈", "Issue report"),
  UNKNOWN("❓", "Unknown file");

  private final String icon;
  private final String text;

  FileType(String icon, String text) {
    this.icon = icon;
    this.text = text;
  }

  /**
   * Emoji (icon) for the given type
   */
  public String icon() {
    return icon;
  }

  public String text() {
    return text;
  }

  /**
   * Return {@code true} if the the file is an INPUT data file. This is GTFS, Netex, OpenStreetMap,
   * and elevation data files. Config files is not data sources and graphs are not considered input
   * data files.
   * <p>
   * At least one input data file must be present to build a graph.
   */
  public boolean isInputDataSource() {
    return EnumSet.of(GTFS, NETEX, OSM, DEM).contains(this);
  }

  /**
   * Return {@code true} if the the file is an OUTPUT data file/directory. This is the graph files
   * and the build-report file.
   */
  public boolean isOutputDataSource() {
    return EnumSet.of(GRAPH, REPORT).contains(this);
  }

  /**
   * @return true if GTFS or NETEX file type.
   */
  public boolean isTransit() {
    return EnumSet.of(GTFS, NETEX).contains(this);
  }
}
