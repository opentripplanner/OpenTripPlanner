package org.opentripplanner.openstreetmap.wayproperty.specifier;

import static org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult.EXACT;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult.NONE;

import javax.annotation.Nonnull;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public sealed interface Condition {
  @Nonnull
  static MatchResult getMatchResult(OSMWithTags way, String opKey, String opValue) {
    if (opValue.equals("*") && way.hasTag(opKey)) {
      return MatchResult.WILDCARD;
    } else if (way.matchesKeyValue(opKey, opValue)) {
      return EXACT;
    } else if (opValue.contains(":")) {
      // treat cases like cobblestone:flattened as cobblestone if a more-specific match
      // does not apply
      var splitValue = opValue.split(":", 2)[0];
      if (way.matchesKeyValue(opKey, splitValue)) {
        return MatchResult.PREFIX;
      } else {
        return NONE;
      }
    } else {
      return NONE;
    }
  }

  default boolean matches(OSMWithTags way) {
    return match(way) == EXACT;
  }

  default boolean isWildcard() {
    return false;
  }

  /**
   * Test to what degree the OSM entity matches with this operation when taking the regular tag keys
   * into account.
   */
  MatchResult match(OSMWithTags way);

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':left' key
   * suffixes into account.
   * <p>
   * For example, it should not match a way with `cycleway:right=lane` when the `cycleway=lane` was
   * required but `cycleway:left=lane` should match.
   */
  MatchResult matchLeft(OSMWithTags way);

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':right' key
   * suffixes into account.
   * <p>
   * For example, it should not match a way with `cycleway:left=lane` when the `cycleway=lane` was
   * required but `cycleway:right=lane` should match.
   */
  MatchResult matchRight(OSMWithTags way);

  enum MatchResult {
    EXACT,
    PREFIX,
    WILDCARD,
    NONE;

    public MatchResult ifNone(MatchResult ifNone) {
      if (this == NONE) {
        return ifNone;
      } else {
        return this;
      }
    }
  }

  record Equals(String key, String value) implements Condition {
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
      return getOneSideResult(way, leftKey);
    }

    @Override
    public MatchResult matchRight(OSMWithTags way) {
      var rightKey = key + ":right";
      return getOneSideResult(way, rightKey);
    }

    private MatchResult getOneSideResult(OSMWithTags way, String oneSideKey) {
      var hasOneSideKey = way.hasTag(oneSideKey);
      var oneSideResult = getMatchResult(way, oneSideKey, value);
      var mainRes = match(way);

      if (hasOneSideKey) {
        return oneSideResult;
      } else {
        return oneSideResult.ifNone(mainRes);
      }
    }
  }

  record Absent(String key) implements Condition {
    @Override
    public MatchResult match(OSMWithTags way) {
      if (way.hasTag(key)) {
        return NONE;
      } else {
        return EXACT;
      }
    }

    @Override
    public MatchResult matchLeft(OSMWithTags way) {
      throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public MatchResult matchRight(OSMWithTags way) {
      throw new UnsupportedOperationException("Not implemented.");
    }
  }

  record GreaterThan(String key, int value) implements Condition {
    @Override
    public MatchResult match(OSMWithTags way) {
      var maybeInt = way.getTagAsInt(key, ignored -> {});
      if (maybeInt.isPresent() && maybeInt.getAsInt() > value) {
        return EXACT;
      } else {
        return NONE;
      }
    }

    @Override
    public MatchResult matchLeft(OSMWithTags way) {
      throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public MatchResult matchRight(OSMWithTags way) {
      throw new UnsupportedOperationException("Not implemented.");
    }
  }

  record LessThan(String key, int value) implements Condition {
    @Override
    public MatchResult match(OSMWithTags way) {
      var maybeInt = way.getTagAsInt(key, ignored -> {});
      if (maybeInt.isPresent() && maybeInt.getAsInt() < value) {
        return EXACT;
      } else {
        return NONE;
      }
    }

    @Override
    public MatchResult matchLeft(OSMWithTags way) {
      throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public MatchResult matchRight(OSMWithTags way) {
      throw new UnsupportedOperationException("Not implemented.");
    }
  }
}
