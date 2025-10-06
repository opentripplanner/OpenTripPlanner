package org.opentripplanner.osm.model;

import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.issues.FloorNumberUnknownAssumedGroundLevel;

public class OsmLevel implements Comparable<OsmLevel> {

  public static final OsmLevel DEFAULT = new OsmLevel(0.0, "default level", Source.NONE, true);

  public final double floorNumber; // 0-based
  public final String name;
  public final Source source;
  public final boolean reliable;

  public OsmLevel(Double floorNumber, String name, Source source, boolean reliable) {
    this.floorNumber = floorNumber;
    this.name = name;
    this.source = source;
    this.reliable = reliable;
  }

  /**
   * Create an OsmLevel from either the 'level' or 'layer' tag.
   */
  public static OsmLevel fromString(
    String levelTag,
    @Nullable String refTag,
    Source source,
    DataImportIssueStore issueStore,
    OsmEntity osmObj
  ) {
    String name = levelTag;
    if (name.startsWith("+")) {
      name = name.substring(1);
    }

    /* try to parse a floor number out of the tag */
    Double floorNumber = null;
    try {
      floorNumber = Double.parseDouble(name);
    } catch (NumberFormatException e) {}

    /* signal failure to extract any useful level information */
    boolean reliable = true;
    if (floorNumber == null) {
      floorNumber = 0.0;
      issueStore.add(new FloorNumberUnknownAssumedGroundLevel(levelTag, osmObj));
      reliable = false;
    }

    name = refTag != null ? refTag : name;
    return new OsmLevel(floorNumber, name, source, reliable);
  }

  @Override
  public int compareTo(OsmLevel other) {
    return Double.compare(this.floorNumber, other.floorNumber);
  }

  @Override
  public int hashCode() {
    return Double.hashCode(this.floorNumber);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (!(other instanceof OsmLevel)) {
      return false;
    }
    return this.floorNumber == ((OsmLevel) other).floorNumber;
  }

  public enum Source {
    LEVEL_TAG,
    LAYER_TAG,
    NONE,
  }
}
