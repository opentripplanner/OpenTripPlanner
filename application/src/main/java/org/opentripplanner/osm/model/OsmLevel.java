package org.opentripplanner.osm.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.issues.FloorNumberUnknownAssumedGroundLevel;
import org.opentripplanner.osm.issues.FloorNumberUnknownGuessedFromAltitude;

public class OsmLevel implements Comparable<OsmLevel> {

  public static final Pattern RANGE_PATTERN = Pattern.compile("^[0-9]+-[0-9]+$");
  public static final double METERS_PER_FLOOR = 3;
  public static final OsmLevel DEFAULT = new OsmLevel(
    0,
    0.0,
    "default level",
    "default level",
    Source.NONE,
    true
  );
  public final int floorNumber; // 0-based
  public final double altitudeMeters;
  public final String shortName; // localized (potentially 1-based)
  public final String longName; // localized (potentially 1-based)
  public final Source source;
  public final boolean reliable;

  public OsmLevel(
    int floorNumber,
    double altitudeMeters,
    String shortName,
    String longName,
    Source source,
    boolean reliable
  ) {
    this.floorNumber = floorNumber;
    this.altitudeMeters = altitudeMeters;
    this.shortName = shortName;
    this.longName = longName;
    this.source = source;
    this.reliable = reliable;
  }

  /**
   * makes an OSMLevel from one of the semicolon-separated fields in an OSM level map relation's
   * levels= tag.
   */
  public static OsmLevel fromString(
    String spec,
    Source source,
    boolean incrementNonNegative,
    DataImportIssueStore issueStore,
    OsmEntity osmObj
  ) {
    /*  extract any altitude information after the @ character */
    Double altitude = null;
    boolean reliable = true;
    int lastIndexAt = spec.lastIndexOf('@');
    if (lastIndexAt != -1) {
      try {
        altitude = Double.parseDouble(spec.substring(lastIndexAt + 1));
      } catch (NumberFormatException e) {}
      spec = spec.substring(0, lastIndexAt);
    }

    /* get short and long level names by splitting on = character */
    String shortName = "";
    String longName = "";
    int indexEquals = spec.indexOf('=');
    if (indexEquals >= 1) {
      shortName = spec.substring(0, indexEquals);
      longName = spec.substring(indexEquals + 1);
    } else {
      // set them both the same, the trailing @altitude has already been discarded
      shortName = longName = spec;
    }
    if (longName.startsWith("+")) {
      longName = longName.substring(1);
    }
    if (shortName.startsWith("+")) {
      shortName = shortName.substring(1);
    }

    /* try to parse a floor number out of names */
    Integer floorNumber = null;
    try {
      floorNumber = Integer.parseInt(longName);
      if (incrementNonNegative) {
        if (source == Source.LEVEL_MAP) {
          if (floorNumber >= 1) floorNumber -= 1; // level maps are localized, floor numbers are 0-based
        } else {
          if (floorNumber >= 0) longName = Integer.toString(floorNumber + 1); // level and layer tags are 0-based
        }
      }
    } catch (NumberFormatException e) {}
    try {
      // short name takes precedence over long name for floor numbering
      floorNumber = Integer.parseInt(shortName);
      if (incrementNonNegative) {
        if (source == Source.LEVEL_MAP) {
          if (floorNumber >= 1) floorNumber -= 1; // level maps are localized, floor numbers are 0-based
        } else {
          if (floorNumber >= 0) shortName = Integer.toString(floorNumber + 1); // level and layer tags are 0-based
        }
      }
    } catch (NumberFormatException e) {}

    /* fall back on altitude when necessary */
    if (floorNumber == null && altitude != null) {
      floorNumber = (int) (altitude / METERS_PER_FLOOR);
      issueStore.add(new FloorNumberUnknownGuessedFromAltitude(spec, floorNumber, osmObj));
      reliable = false;
    }

    /* set default value if parsing failed */
    if (altitude == null) {
      altitude = 0.0;
    }
    /* signal failure to extract any useful level information */
    if (floorNumber == null) {
      floorNumber = 0;
      issueStore.add(new FloorNumberUnknownAssumedGroundLevel(spec, osmObj));
      reliable = false;
    }
    return new OsmLevel(floorNumber, altitude, shortName, longName, source, reliable);
  }

  public static List<OsmLevel> fromSpecList(
    String specList,
    Source source,
    boolean incrementNonNegative,
    DataImportIssueStore issueStore,
    OsmEntity osmObj
  ) {
    List<String> levelSpecs = new ArrayList<>();

    Matcher m;
    for (String level : specList.split(";")) {
      m = RANGE_PATTERN.matcher(level);
      if (m.matches()) { // this field specifies a range of levels
        String[] range = level.split("-");
        int endOfRange = Integer.parseInt(range[1]);
        for (int i = Integer.parseInt(range[0]); i <= endOfRange; i++) {
          levelSpecs.add(Integer.toString(i));
        }
      } else { // this field is not a range, just a single level
        levelSpecs.add(level);
      }
    }

    /* build an OSMLevel for each level spec in the list */
    List<OsmLevel> levels = new ArrayList<>();
    for (String spec : levelSpecs) {
      levels.add(fromString(spec, source, incrementNonNegative, issueStore, osmObj));
    }
    return levels;
  }

  public static Map<String, OsmLevel> mapFromSpecList(
    String specList,
    Source source,
    boolean incrementNonNegative,
    DataImportIssueStore issueStore,
    OsmEntity osmObj
  ) {
    Map<String, OsmLevel> map = new HashMap<>();
    for (OsmLevel level : fromSpecList(
      specList,
      source,
      incrementNonNegative,
      issueStore,
      osmObj
    )) {
      map.put(level.shortName, level);
    }
    return map;
  }

  @Override
  public int compareTo(OsmLevel other) {
    return this.floorNumber - other.floorNumber;
  }

  @Override
  public int hashCode() {
    return this.floorNumber;
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
    LEVEL_MAP,
    LEVEL_TAG,
    LAYER_TAG,
    ALTITUDE,
    NONE,
  }
}
