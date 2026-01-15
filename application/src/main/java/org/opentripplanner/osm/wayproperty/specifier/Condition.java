package org.opentripplanner.osm.wayproperty.specifier;

import static org.opentripplanner.osm.wayproperty.specifier.Condition.MatchResult.EXACT;
import static org.opentripplanner.osm.wayproperty.specifier.Condition.MatchResult.NONE;
import static org.opentripplanner.osm.wayproperty.specifier.Condition.MatchResult.WILDCARD;

import java.util.Arrays;
import org.opentripplanner.osm.model.OsmEntity;

public sealed interface Condition {
  String key();

  default MatchResult matchType() {
    return EXACT;
  }

  boolean isExtendedKeyMatch(OsmEntity way, String exKey);

  /**
   * Test to what degree the OSM entity matches with this operation when taking the regular tag keys
   * into account.
   */
  default boolean isMatch(OsmEntity way) {
    return isExtendedKeyMatch(way, this.key());
  }

  default MatchResult match(OsmEntity way) {
    return isMatch(way) ? matchType() : NONE;
  }

  /**
   * Test to what degree the OSM entity matches with this operation when taking the ':left' key
   * suffixes into account.
   * <p>
   * For example, it should not match a way with `cycleway:right=lane` when the `cycleway=lane` was
   * required but `cycleway:left=lane` should match.
   */
  default boolean isLeftMatch(OsmEntity way) {
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
  default boolean isRightMatch(OsmEntity way) {
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
  default boolean isExplicitBothMatch(OsmEntity way) {
    var bothKey = this.key() + ":both";
    if (way.hasTag(bothKey)) {
      return isExtendedKeyMatch(way, bothKey);
    } else {
      return isMatch(way);
    }
  }

  default boolean isForwardMatch(OsmEntity way) {
    var forwardKey = this.key() + ":forward";
    if (way.hasTag(forwardKey)) {
      return isExtendedKeyMatch(way, forwardKey);
    } else {
      /* Assumes right hand traffic */
      return isRightMatch(way);
    }
  }

  default MatchResult matchForward(OsmEntity way) {
    return isForwardMatch(way) ? matchType() : NONE;
  }

  default boolean isBackwardMatch(OsmEntity way) {
    var backwardKey = this.key() + ":backward";
    if (way.hasTag(backwardKey)) {
      return isExtendedKeyMatch(way, backwardKey);
    } else {
      /* Assumes right hand traffic */
      return isLeftMatch(way);
    }
  }

  default MatchResult matchBackward(OsmEntity way) {
    return isBackwardMatch(way) ? matchType() : NONE;
  }

  enum MatchResult {
    EXACT,
    WILDCARD,
    NONE,
  }

  /**
   * Negates the condition
   */
  record Not(Condition condition) implements Condition {
    @Override
    public String key() {
      return condition.key();
    }

    @Override
    public boolean isExtendedKeyMatch(OsmEntity way, String exKey) {
      return !condition.isExtendedKeyMatch(way, exKey);
    }

    @Override
    public String toString() {
      return "not(%s)".formatted(condition.toString());
    }
  }

  /**
   * Selects tags where a given key/value matches.
   */
  record Equals(String key, String value) implements Condition {
    @Override
    public boolean isExtendedKeyMatch(OsmEntity way, String exKey) {
      return way.hasTag(exKey) && way.isTag(exKey, value);
    }

    @Override
    public String toString() {
      return "%s=%s".formatted(key, value);
    }
  }

  /**
   * Selects tags with a given key.
   */
  record Present(String key) implements Condition {
    @Override
    public MatchResult matchType() {
      return WILDCARD;
    }
    @Override
    public boolean isExtendedKeyMatch(OsmEntity way, String exKey) {
      return way.hasTag(exKey);
    }

    @Override
    public String toString() {
      return "present(%s)".formatted(key);
    }
  }

  /**
   * Selects tags where a given tag is absent.
   */
  record Absent(String key) implements Condition {
    @Override
    public boolean isExtendedKeyMatch(OsmEntity way, String exKey) {
      return !way.hasTag(exKey);
    }

    @Override
    public String toString() {
      return "!%s".formatted(key);
    }
  }

  /**
   * Selects tags where the integer value is greater than a given number.
   */
  record GreaterThan(String key, int value) implements Condition {
    @Override
    public boolean isExtendedKeyMatch(OsmEntity way, String exKey) {
      var maybeInt = way.getTagAsInt(exKey, ignored -> {});
      return maybeInt.isPresent() && maybeInt.getAsInt() > value;
    }

    @Override
    public String toString() {
      return "%s > %s".formatted(key, value);
    }
  }

  /**
   * Selects tags where the integer value is less than a given number.
   */
  record LessThan(String key, int value) implements Condition {
    @Override
    public boolean isExtendedKeyMatch(OsmEntity way, String exKey) {
      var maybeInt = way.getTagAsInt(exKey, ignored -> {});
      return maybeInt.isPresent() && maybeInt.getAsInt() < value;
    }

    @Override
    public String toString() {
      return "%s < %s".formatted(key, value);
    }
  }

  /**
   * Selects integer tag values and checks if they are in between a lower and an upper bound.
   */
  record InclusiveRange(String key, int upper, int lower) implements Condition {
    public InclusiveRange {
      if (upper < lower) {
        throw new IllegalArgumentException("Upper bound is lower than lower bound");
      }
    }

    @Override
    public boolean isExtendedKeyMatch(OsmEntity way, String exKey) {
      var maybeInt = way.getTagAsInt(exKey, ignored -> {});
      return maybeInt.isPresent() && maybeInt.getAsInt() >= lower && maybeInt.getAsInt() <= upper;
    }

    @Override
    public String toString() {
      return "%s > %s < %s".formatted(lower, key, upper);
    }
  }

  /**
   * Selects a tag which has one of a set of given values.
   */
  record OneOf(String key, String... values) implements Condition {
    @Override
    public boolean isExtendedKeyMatch(OsmEntity way, String exKey) {
      return Arrays.stream(values).anyMatch(value -> way.isTag(exKey, value));
    }

    @Override
    public String toString() {
      return "%s one of [%s]".formatted(key, String.join(", ", values));
    }
  }

  /**
   * Selects a tag where one of the following conditions is true:
   *  - one of a set of given values matches
   *  - the tag is absent
   */
  record OneOfOrAbsent(String key, String... values) implements Condition {
    /* A use case for this is to detect the absence of a sidewalk, cycle lane or verge*/
    public OneOfOrAbsent(String key) {
      this(key, "no", "none");
    }

    @Override
    public boolean isExtendedKeyMatch(OsmEntity way, String exKey) {
      return (
        !way.hasTag(exKey) || Arrays.stream(values).anyMatch(value -> way.isTag(exKey, value))
      );
    }

    @Override
    public String toString() {
      return "%s not one of [%s] or absent".formatted(key, String.join(", ", values));
    }
  }
}
