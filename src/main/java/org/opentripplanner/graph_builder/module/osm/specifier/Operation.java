package org.opentripplanner.graph_builder.module.osm.specifier;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

public interface Operation {
  boolean matches(OSMWithTags way);

  default boolean matchesRight(OSMWithTags way) {
    return matches(way);
  }

  default boolean matchesLeft(OSMWithTags way) {
    return matches(way);
  }

  boolean isWildcard();

  String key();

  String value();

  record Equals(String key, String value) implements Operation {
    @Override
    public boolean matches(OSMWithTags way) {
      return way.hasTag(key) && way.getTag(key).equals(value);
    }

    @Override
    public boolean isWildcard() {
      return value.equals("*");
    }
  }

  record LeftRightEquals(String key, String value) implements Operation {
    @Override
    public boolean matches(OSMWithTags way) {
      return way.hasTag(key) && way.getTag(key).equals(value);
    }

    @Override
    public boolean isWildcard() {
      return value.equals("*");
    }

    @Override
    public boolean matchesLeft(OSMWithTags way) {
      var leftKey = key + ":left";
      return way.hasTag(leftKey) && way.getTag(leftKey).equals(value);
    }

    @Override
    public boolean matchesRight(OSMWithTags way) {
      var rightKey = key + ":right";
      return way.hasTag(rightKey) && way.getTag(rightKey).equals(value);
    }
  }
}
