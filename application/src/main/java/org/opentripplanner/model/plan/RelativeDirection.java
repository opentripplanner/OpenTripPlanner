package org.opentripplanner.model.plan;

/**
 * Represents a turn direction, relative to the current heading.
 * <p>
 * CIRCLE_CLOCKWISE and CIRCLE_CLOCKWISE are used to represent traffic circles.
 */
public enum RelativeDirection {
  DEPART,
  HARD_LEFT,
  LEFT,
  SLIGHTLY_LEFT,
  CONTINUE,
  SLIGHTLY_RIGHT,
  RIGHT,
  HARD_RIGHT,
  CIRCLE_CLOCKWISE,
  CIRCLE_COUNTERCLOCKWISE,
  ELEVATOR,
  UTURN_LEFT,
  UTURN_RIGHT,
  ENTER_STATION,
  EXIT_STATION,
  /**
   * We don't have a way to reliably tell if we are entering or exiting a station and therefore
   * use this generic enum value. Please don't expose it in APIs.
   * <p>
   * If we manage to figure it out in the future, we can remove this.
   */
  ENTER_OR_EXIT_STATION,
  FOLLOW_SIGNS;

  public static RelativeDirection calculate(
    double lastAngle,
    double thisAngle,
    boolean roundabout
  ) {
    return calculate(thisAngle - lastAngle, roundabout);
  }

  public static RelativeDirection calculate(double angle, boolean roundabout) {
    double cwAngle = angle;
    if (cwAngle < 0) {
      cwAngle += Math.PI * 2;
    }
    double ccwAngle = Math.PI * 2 - cwAngle;

    if (roundabout) {
      // roundabout: the direction we turn onto it implies the circling direction
      if (cwAngle > ccwAngle) {
        return RelativeDirection.CIRCLE_CLOCKWISE;
      } else {
        return RelativeDirection.CIRCLE_COUNTERCLOCKWISE;
      }
    }

    // less than 0.3 rad counts as straight, to simplify walking instructions
    if (cwAngle < 0.3 || ccwAngle < 0.3) {
      return RelativeDirection.CONTINUE;
    } else if (cwAngle < 0.7) {
      return RelativeDirection.SLIGHTLY_RIGHT;
    } else if (ccwAngle < 0.7) {
      return RelativeDirection.SLIGHTLY_LEFT;
    } else if (cwAngle < 2) {
      return RelativeDirection.RIGHT;
    } else if (ccwAngle < 2) {
      return RelativeDirection.LEFT;
    } else if (cwAngle < Math.PI) {
      return RelativeDirection.HARD_RIGHT;
    } else {
      return RelativeDirection.HARD_LEFT;
    }
  }
}
