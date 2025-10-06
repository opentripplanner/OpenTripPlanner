package org.opentripplanner.osm.model;

import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.issues.FloorNumberUnknownAssumedGroundLevel;

public class OsmLevelFactory {

  public static final OsmLevel DEFAULT = new OsmLevel(0.0, "default level");

  private final DataImportIssueStore issueStore;

  public OsmLevelFactory(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  /**
   * Get level for a way by parsing the 'level' and 'layer' tags.
   * If the level is parsed from the 'level' tag, the 'level:ref' tag is used for a name.
   */
  public OsmLevel createOsmLevelForWay(OsmEntity way) {
    OsmLevel level = DEFAULT;
    if (way.hasTag("level")) {
      String levelTag = way.getTag("level");
      String refTag = way.getTag("level:ref");
      level = createOsmLevelFromTag(levelTag, refTag, way);
    } else if (way.hasTag("layer")) {
      String levelTag = way.getTag("layer");
      level = createOsmLevelFromTag(levelTag, null, way);
    }
    return level;
  }

  /**
   * Create an OsmLevel from a tag with an optional ref tag to be used as a name.
   */
  private OsmLevel createOsmLevelFromTag(
    String levelTag,
    @Nullable String refTag,
    OsmEntity osmObj
  ) {
    String name = levelTag;
    if (name.startsWith("+")) {
      name = name.substring(1);
    }

    // Try to parse a level out of the tag.
    Double level = null;
    try {
      level = Double.parseDouble(name);
    } catch (NumberFormatException e) {}

    // Signal failure to extract any useful level information.
    if (level == null) {
      issueStore.add(new FloorNumberUnknownAssumedGroundLevel(levelTag, osmObj));
      return DEFAULT;
    }

    name = refTag != null ? refTag : name;
    return new OsmLevel(level, name);
  }
}
