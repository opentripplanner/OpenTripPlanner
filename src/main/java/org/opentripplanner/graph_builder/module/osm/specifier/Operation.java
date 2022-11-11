package org.opentripplanner.graph_builder.module.osm.specifier;

import javax.annotation.Nonnull;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public interface Operation {
  @Nonnull
  static MatchResult getMatchResult(OSMWithTags way, String opKey, String opValue) {
    if (opValue.equals("*") && way.hasTag(opKey)) {
      return MatchResult.WILDCARD;
    } else if (way.matchesKeyValue(opKey, opValue)) {
      return MatchResult.EXACT;
    } else if (opValue.contains(":")) {
      // treat cases like cobblestone:flattened as cobblestone if a more-specific match
      // does not apply
      var splitValue = opValue.split(":", 2)[0];
      if (way.matchesKeyValue(opKey, splitValue)) {
        return MatchResult.PREFIX;
      } else {
        return MatchResult.NONE;
      }
    } else {
      return MatchResult.NONE;
    }
  }

  default boolean matches(OSMWithTags way) {
    return match(way) == MatchResult.EXACT;
  }

  boolean isWildcard();

  MatchResult match(OSMWithTags way);

  MatchResult matchLeft(OSMWithTags way);

  MatchResult matchRight(OSMWithTags way);

  enum MatchResult {
    EXACT,
    PREFIX,
    WRONG_DIRECTION,
    WILDCARD,

    NONE;

    public MatchResult ifNone(MatchResult ifNone) {
      if (this.ordinal() == NONE.ordinal()) {
        return ifNone;
      } else {
        return this;
      }
    }
  }

  record LeftRightEquals(String key, String value) implements Operation {
    @Override
    public boolean isWildcard() {
      return value.equals("*");
    }

    @Override
    public MatchResult match(OSMWithTags way) {
      return getMatchResult(way, key, value);
    }

    @Override
    public MatchResult matchLeft(OSMWithTags way) {
      var leftKey = key + ":left";
      return getMatchResult(way, leftKey, value);
    }

    @Override
    public MatchResult matchRight(OSMWithTags way) {
      var rightKey = key + ":right";
      return getMatchResult(way, rightKey, value);
    }
  }
}
