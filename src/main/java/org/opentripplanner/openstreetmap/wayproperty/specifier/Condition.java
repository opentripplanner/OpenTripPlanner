package org.opentripplanner.openstreetmap.wayproperty.specifier;

import static org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult.EXACT;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult.NONE;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult.WILDCARD;

import java.util.Arrays;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public sealed interface Condition {
  String key();

  default MatchResult matchType() {
    return EXACT;
  }

  boolean isExtendedKeyMatch(OSMWithTags way, String exKey);

  /**
   * Test to what degree the OSM entity matches with this operation when taking the regular tag keys
   * into account.
   */
  default boolean isMatch(OSMWithTags way) {
    return isExtendedKeyMatch(way, this.key());
  }

  default MatchResult match(OSMWithTags way) {
    return isMatch(way) ? matchType() : NONE;
  }

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':left' key
   * suffixes into account.
   * <p>
   * For example, it should not match a way with `cycleway:right=lane` when the `cycleway=lane` was
   * required but `cycleway:left=lane` should match.
   */
  default boolean isLeftMatch(OSMWithTags way) {
    var leftKey = this.key() + ":left";
    if (way.hasTag(leftKey)) {
      return isExtendedKeyMatch(way, leftKey);
    } else {
      return isExplicitBothMatch(way);
    }
  }

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':right' key
   * suffixes into account.
   * <p>
   * For example, it should not match a way with `cycleway:left=lane` when the `cycleway=lane` was
   * required but `cycleway:right=lane` should match.
   */
  default boolean isRightMatch(OSMWithTags way) {
    var rightKey = this.key() + ":right";
    if (way.hasTag(rightKey)) {
      return isExtendedKeyMatch(way, rightKey);
    } else {
      return isExplicitBothMatch(way);
    }
  }

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':both' key
   * suffixes into account.
   */
  default boolean isExplicitBothMatch(OSMWithTags way) {
    var bothKey = this.key() + ":both";
    if (way.hasTag(bothKey)) {
      return isExtendedKeyMatch(way, bothKey);
    } else {
      return isMatch(way);
    }
  }

  default boolean isForwardMatch(OSMWithTags way) {
    var forwardKey = this.key() + ":forward";
    if (way.hasTag(forwardKey)) {
      return isExtendedKeyMatch(way, forwardKey);
    } else {
      /* Assumes right hand traffic */
      return isRightMatch(way);
    }
  }

  default MatchResult matchForward(OSMWithTags way) {
    return isForwardMatch(way) ? matchType() : NONE;
  }

  default boolean isBackwardMatch(OSMWithTags way) {
    var backwardKey = this.key() + ":backward";
    if (way.hasTag(backwardKey)) {
      return isExtendedKeyMatch(way, backwardKey);
    } else {
      /* Assumes right hand traffic */
      return isLeftMatch(way);
    }
  }

  default MatchResult matchBackward(OSMWithTags way) {
    return isBackwardMatch(way) ? matchType() : NONE;
  }

  enum MatchResult {
    EXACT,
    WILDCARD,
    NONE,
  }

  record Equals(String key, String value) implements Condition {
    @Override
    public boolean isExtendedKeyMatch(OSMWithTags way, String exKey) {
      return way.hasTag(exKey) && way.isTag(exKey, value);
    }
  }

  record Present(String key) implements Condition {
    @Override
    public MatchResult matchType() {
      return WILDCARD;
    }
    @Override
    public boolean isExtendedKeyMatch(OSMWithTags way, String exKey) {
      return way.hasTag(exKey);
    }
  }

  record Absent(String key) implements Condition {
    @Override
    public boolean isExtendedKeyMatch(OSMWithTags way, String exKey) {
      return !way.hasTag(exKey);
    }
  }

  record GreaterThan(String key, int value) implements Condition {
    @Override
    public boolean isExtendedKeyMatch(OSMWithTags way, String exKey) {
      var maybeInt = way.getTagAsInt(exKey, ignored -> {});
      return maybeInt.isPresent() && maybeInt.getAsInt() > value;
    }
  }

  record LessThan(String key, int value) implements Condition {
    @Override
    public boolean isExtendedKeyMatch(OSMWithTags way, String exKey) {
      var maybeInt = way.getTagAsInt(exKey, ignored -> {});
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
    public boolean isExtendedKeyMatch(OSMWithTags way, String exKey) {
      var maybeInt = way.getTagAsInt(exKey, ignored -> {});
      return maybeInt.isPresent() && maybeInt.getAsInt() >= lower && maybeInt.getAsInt() <= upper;
    }
  }

  record EqualsAnyIn(String key, String... values) implements Condition {
    @Override
    public boolean isExtendedKeyMatch(OSMWithTags way, String exKey) {
      return Arrays.stream(values).anyMatch(value -> way.isTag(exKey, value));
    }
  }

  record EqualsAnyInOrAbsent(String key, String... values) implements Condition {
    /* A use case for this is to detect the absence of a sidewalk, cycle lane or verge*/
    public EqualsAnyInOrAbsent(String key) {
      this(key, "no", "none");
    }

    @Override
    public boolean isExtendedKeyMatch(OSMWithTags way, String exKey) {
      return (
        !way.hasTag(exKey) || Arrays.stream(values).anyMatch(value -> way.isTag(exKey, value))
      );
    }
  }
}
