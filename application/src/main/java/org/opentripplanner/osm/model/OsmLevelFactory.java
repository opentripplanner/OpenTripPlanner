package org.opentripplanner.osm.model;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.issues.FloorNumberUnknownAssumedGroundLevel;
import org.opentripplanner.osm.issues.LevelAndLevelRefDifferentSizes;

public class OsmLevelFactory {

  public static final OsmLevel DEFAULT = new OsmLevel(0.0, "default level");

  private final DataImportIssueStore issueStore;

  public OsmLevelFactory(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  /**
   * Create a list of OsmLevel objects for an entity by parsing the 'level' and 'layer' tags.
   * If the level is parsed from the 'level' tag, the 'level:ref' tag is used for naming.
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
  public List<OsmLevel> createOsmLevelsForEntity(OsmEntity entity) {
    if (entity.hasTag("level")) {
      return createLevelListFromTag(entity.getTag("level"), entity.getTag("level:ref"), entity);
    } else if (entity.hasTag("layer")) {
      return createLevelListFromTag(entity.getTag("layer"), null, entity);
    }
    return List.of();
  }

  private List<OsmLevel> createLevelListFromTag(
    String levelTag,
    @Nullable String nameTag,
    OsmEntity entity
  ) {
    List<OsmLevel> levels;

    String[] levelArray = levelTag.split(";");
    if (nameTag != null) {
      String[] nameArray = nameTag.split(";");
      // If the levelTag and nameTag contain multi-level info, the amount of level names in the
      // nameTag needs to equal the amount of levels in the levelTag.
      // Otherwise the nameTag data won't be used because the names can't be reliably mapped.
      if (levelArray.length == nameArray.length) {
        levels = createLevelListFromSubstringArrays(levelArray, nameArray);
      } else {
        levels = createLevelListFromSubstringArrays(levelArray);
        issueStore.add(
          new LevelAndLevelRefDifferentSizes(levelArray.length, nameArray.length, entity)
        );
      }
    } else {
      levels = createLevelListFromSubstringArrays(levelArray);
    }

    if (levelListIsValid(levels)) {
      return levels;
    } else {
      issueStore.add(new FloorNumberUnknownAssumedGroundLevel(levelTag, entity));
      return List.of();
    }
  }

  private boolean levelListIsValid(List<OsmLevel> levels) {
    for (OsmLevel level : levels) {
      if (level == null) {
        return false;
      }
    }
    return true;
  }

  private List<OsmLevel> createLevelListFromSubstringArrays(
    String[] levelArray,
    String[] nameArray
  ) {
    List<OsmLevel> levels = new ArrayList<>();
    if (nameArray != null) {
      for (int i = 0; i < levelArray.length; i++) {
        levels.add(createOsmLevelFromTagSubstrings(levelArray[i], nameArray[i]));
      }
    }
    return levels;
  }

  private List<OsmLevel> createLevelListFromSubstringArrays(String[] levelArray) {
    List<OsmLevel> levels = new ArrayList<>();
    for (String level : levelArray) {
      levels.add(createOsmLevelFromTagSubstrings(level, null));
    }
    return levels;
  }

  /**
   * Try to create an OsmLevel from part of a level tag
   * with an optional name from part of a tag.
   */
  @Nullable
  private OsmLevel createOsmLevelFromTagSubstrings(
    String levelString,
    @Nullable String nameString
  ) {
    try {
      Double level = Double.parseDouble(levelString);
      if (nameString != null) {
        return new OsmLevel(level, nameString);
      } else {
        return new OsmLevel(level, levelString);
      }
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
