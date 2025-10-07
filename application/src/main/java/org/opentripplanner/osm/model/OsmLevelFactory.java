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
   * <p>
   * The 'level' tag is a zero-based floor number of a feature
   * (where 0 is the ground level and -1 is the basement).
   * See https://wiki.openstreetmap.org/wiki/Key:level.
   * <p>
   * The 'layer' tag is used to mark the vertical relationship between two intersecting features.
   * See https://wiki.openstreetmap.org/wiki/Key:layer.
   * <p>
   * The 'level:ref' tag marks the floor an object is located at,
   * based on the locally used method of indicating a specific floor.
   * See https://wiki.openstreetmap.org/wiki/Key:level:ref.
   */
  @Nullable
  public OsmLevel createOsmLevelForEntity(OsmEntity entity) {
    if (entity.hasTag("level")) {
      return createOsmLevelFromTag(entity.getTag("level"), entity.getTag("level:ref"), entity);
    } else if (entity.hasTag("layer")) {
      return createOsmLevelFromTag(entity.getTag("layer"), null, entity);
    }
    return null;
  }

  /**
   * Create an OsmLevel from a tag with an optional name tag to be used as a name.
   */
  @Nullable
  private OsmLevel createOsmLevelFromTag(
    String levelTag,
    @Nullable String nameTag,
    OsmEntity entity
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
      issueStore.add(new FloorNumberUnknownAssumedGroundLevel(levelTag, entity));
      return null;
    }
  }
}
