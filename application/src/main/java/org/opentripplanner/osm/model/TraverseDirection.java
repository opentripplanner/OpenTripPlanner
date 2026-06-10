package org.opentripplanner.osm.model;

/**
 * Represents the traverse direction for a way in OpenStreetMap.
 * <p>
 * See <a href="https://wiki.openstreetmap.org/wiki/Forward_%26_backward,_left_%26_right">the Wiki page</a>
 * for a detailed explanation
 */
public enum TraverseDirection {
  /**
   * Traverse in the direction the way is defined, from the beginning node to the end node.
   */
  FORWARD(":forward"),
  /**
   * Traverse against the direction the way is defined, from the end node to the beginning node.
   */
  BACKWARD(":backward"),
  /**
   * Traverse not in a specific direction of the way, for example, across an area or through a node.
   */
  DIRECTIONLESS("");

  private final String tagSuffix;

  TraverseDirection(String tagSuffix) {
    this.tagSuffix = tagSuffix;
  }

  public String tagSuffix() {
    return tagSuffix;
  }

  public TraverseDirection reverse() {
    return switch (this) {
      case FORWARD -> BACKWARD;
      case BACKWARD -> FORWARD;
      case DIRECTIONLESS -> DIRECTIONLESS;
    };
  }
}
