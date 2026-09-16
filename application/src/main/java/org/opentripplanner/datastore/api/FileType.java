package org.opentripplanner.datastore.api;

import java.util.EnumSet;
import java.util.Optional;
import javax.annotation.Nullable;

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
  GTFS_TAXI_ZONE("🚕", "Taxi zone data", GTFS),
  NETEX("🚌", "NeTEx data"),
  EMISSION("🌿", "Emission data"),
  EMPIRICAL_DATA("📊", "Empirical data"),
  GRAPH("🌐", "OTP Graph file"),
  REPORT("📈", "Issue report"),
  CACHE("📎", "OTP Cache"),
  UNKNOWN("❓", "Unknown file");

  private final String icon;
  private final String text;

  @Nullable
  private final FileType supertype;

  FileType(String icon, String text) {
    this(icon, text, null);
  }

  FileType(String icon, String text, @Nullable FileType supertype) {
    this.icon = icon;
    this.text = text;
    this.supertype = supertype;
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
   * Return {@code true} if the file is an OUTPUT data file/directory. This is the graph files
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

  /**
   * If present, this type is not resolved directly from a filename or a dedicated build-config
   * file list. If present, this type is essentialy an internal type derived from its supertype.
   */
  public Optional<FileType> supertype() {
    return Optional.ofNullable(supertype);
  }
}
