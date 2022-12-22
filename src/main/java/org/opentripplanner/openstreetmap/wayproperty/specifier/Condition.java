package org.opentripplanner.openstreetmap.wayproperty.specifier;

import static org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult.EXACT;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult.NONE;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult.WILDCARD;

import java.util.Arrays;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public sealed interface Condition {
  boolean getMatches(OSMWithTags way, String opKey);

  default boolean matches(OSMWithTags way) {
    return getMatches(way, this.key());
  }

  String key();

  /**
   * Test to what degree the OSM entity matches with this operation when taking the regular tag keys
   * into account.
   */
  default MatchResult match(OSMWithTags way) {
    return matches(way) ? EXACT : NONE;
  }

  default boolean matchesLeft(OSMWithTags way) {
    var leftKey = this.key() + ":left";
    if (way.hasTag(leftKey)) {
      return getMatches(way, leftKey);
    } else {
      return matchesExplicitBoth(way);
    }
  }

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':left' key
   * suffixes into account.
   * <p>
   * For example, it should not match a way with `cycleway:right=lane` when the `cycleway=lane` was
   * required but `cycleway:left=lane` should match.
   */
  default MatchResult matchLeft(OSMWithTags way) {
    return matchesLeft(way) ? EXACT : NONE;
  }

  default boolean matchesRight(OSMWithTags way) {
    var rightKey = this.key() + ":right";
    if (way.hasTag(rightKey)) {
      return getMatches(way, rightKey);
    } else {
      return matchesExplicitBoth(way);
    }
  }

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':right' key
   * suffixes into account.
   * <p>
   * For example, it should not match a way with `cycleway:left=lane` when the `cycleway=lane` was
   * required but `cycleway:right=lane` should match.
   */
  default MatchResult matchRight(OSMWithTags way) {
    return matchesRight(way) ? EXACT: NONE;
  }

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':both' key
   * suffixes into account.
   */
  default boolean matchesExplicitBoth(OSMWithTags way) {
    var bothKey = this.key() + ":both";
    if (way.hasTag(bothKey)) {
      return getMatches(way, bothKey);
    } else {
      return matches(way);
    }
  }

  enum MatchResult {
    EXACT,
    WILDCARD,
    NONE
  }

  record Equals(String key, String value) implements Condition {
    @Override
    public boolean getMatches(OSMWithTags way, String opKey) {
      return way.hasTag(opKey) && way.matchesKeyValue(opKey, value);
    }
  }

  record Present(String key) implements Condition {
    @Override
    public MatchResult match(OSMWithTags way) {
      return matches(way) ? WILDCARD : NONE;
    }
    @Override
    public MatchResult matchLeft(OSMWithTags way) {
      return matchesLeft(way) ? WILDCARD : NONE;
    }

    @Override
    public MatchResult matchRight(OSMWithTags way) {
      return matchesRight(way) ? WILDCARD : NONE;
    }

    @Override
    public boolean getMatches(OSMWithTags way, String opKey) {
      return way.hasTag(opKey);
    }
  }

  record Absent(String key) implements Condition {
    @Override
    public boolean getMatches(OSMWithTags way, String opKey) {
      return !way.hasTag(opKey);
    }
  }

  record GreaterThan(String key, int value) implements Condition {
    @Override
    public boolean getMatches(OSMWithTags way, String opKey) {
      var maybeInt = way.getTagAsInt(opKey, ignored -> {});
      return maybeInt.isPresent() && maybeInt.getAsInt() > value;
    }
  }

  record LessThan(String key, int value) implements Condition {
    @Override
    public boolean getMatches(OSMWithTags way, String opKey) {
      var maybeInt = way.getTagAsInt(opKey, ignored -> {});
      return maybeInt.isPresent() && maybeInt.getAsInt() < value;
    }
  }

  record InclusiveRange(String key, int upper, int lower) implements Condition {
    public InclusiveRange {
      if (upper < lower) {
        throw new IllegalArgumentException("Upper bound is lower than lower bound");
      }
    }

    @Override
    public boolean getMatches(OSMWithTags way, String opKey) {
      var maybeInt = way.getTagAsInt(opKey, ignored -> {});
      return maybeInt.isPresent() && maybeInt.getAsInt() >= lower && maybeInt.getAsInt() <= upper;
    }
  }

  record EqualsAnyIn(String key, String... values) implements Condition {
    @Override
    public boolean getMatches(OSMWithTags way, String opKey) {
      return Arrays.stream(values).anyMatch(value -> way.matchesKeyValue(opKey, value));
    }
  }
}
