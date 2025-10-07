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
   * Create an OsmLevel for an entity by parsing the 'level' and 'layer' tags.
   * If the level is parsed from the 'level' tag, the 'level:ref' tag is used for a name.
   */
  @Nullable
  public OsmLevel createOsmLevelForEntity(OsmEntity way) {
    if (way.hasTag("level")) {
      return createOsmLevelFromTag(way.getTag("level"), way.getTag("level:ref"), way);
    } else if (way.hasTag("layer")) {
      return createOsmLevelFromTag(way.getTag("layer"), null, way);
    }
    return null;
  }

  /**
   * Create an OsmLevel from a tag with an optional ref tag to be used as a name.
   */
  @Nullable
  private OsmLevel createOsmLevelFromTag(
    String levelTag,
    @Nullable String nameTag,
    OsmEntity osmObj
  ) {
    // Try to parse a level out of the levelTag.
    try {
      Double level = Double.parseDouble(levelTag);
      if (nameTag != null) {
        return new OsmLevel(level, nameTag);
      } else {
        return new OsmLevel(level, levelTag);
      }
    } catch (NumberFormatException e) {
      issueStore.add(new FloorNumberUnknownAssumedGroundLevel(levelTag, osmObj));
      return null;
    }
  }
}
