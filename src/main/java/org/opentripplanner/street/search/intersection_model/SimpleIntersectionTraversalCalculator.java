package org.opentripplanner.street.search.intersection_model;

import java.io.Serializable;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.TraverseMode;

public class SimpleIntersectionTraversalCalculator
  extends AbstractIntersectionTraversalCalculator
  implements Serializable {

  private final DrivingDirection drivingDirection;

  private final double acrossTrafficBicycleTurnMultiplier = getSafeBicycleTurnModifier() * 3;

  public SimpleIntersectionTraversalCalculator(DrivingDirection drivingDirection) {
    this.drivingDirection = drivingDirection;
  }

  @Override
  public double computeTraversalDuration(
    IntersectionVertex v,
    StreetEdge from,
    StreetEdge to,
    TraverseMode mode,
    float fromSpeed,
    float toSpeed
  ) {
    // If the vertex is free-flowing then (by definition) there is no duration to traverse it.
    if (v.inferredFreeFlowing()) {
      return 0;
    }

    if (mode.isDriving()) {
      return computeDrivingTraversalDuration(v, from, to);
    } else if (mode.isCycling()) {
      return computeCyclingTraversalDuration(from, to, toSpeed);
    } else {
      return computeNonDrivingTraversalDuration(from, to, toSpeed);
    }
  }

  public int getMinRightTurnAngle() {
    return 45;
  }

  public int getMaxRightTurnAngle() {
    return 135;
  }

  public int getMinLeftTurnAngle() {
    return 225;
  }

  public int getMaxLeftTurnAngle() {
    return 315;
  }

  /**
   * Expected time it takes to make a right at a light.
   */
  public double getExpectedRightAtLightTimeSec() {
    return 15.0;
  }

  /**
   * Expected time it takes to continue straight at a light.
   */
  public double getExpectedStraightAtLightTimeSec() {
    return 15.0;
  }

  /**
   * Expected time it takes to turn left at a light.
   */
  public double getExpectedLeftAtLightTimeSec() {
    return 15.0;
  }

  /**
   * Expected time it takes to make a right without a stop light.
   */
  public double getExpectedRightNoLightTimeSec() {
    return 8.0;
  }

  /**
   * Expected time it takes to continue straight without a stop light.
   */
  public double getExpectedStraightNoLightTimeSec() {
    return 5.0;
  }

  /**
   * Expected time it takes to turn left without a stop light.
   */
  public double getExpectedLeftNoLightTimeSec() {
    return 8.0;
  }

  public double getSafeBicycleTurnModifier() {
    return 5;
  }

  /**
   * Since doing a left turn on a bike is quite dangerous we add a duration for it
   **/
  public double getAcrossTrafficBicycleTurnMultiplier() {
    return acrossTrafficBicycleTurnMultiplier;
  }

  /**
   * Returns if this angle represents a safe turn were incoming traffic does not have to be
   * crossed.
   * <p>
   * In right hand traffic countries (US, mainland Europe), this is a right turn. In left hand
   * traffic countries (UK, Japan) this is a left turn.
   */
  protected boolean isSafeTurn(int turnAngle) {
    return switch (drivingDirection) {
      case RIGHT -> isRightTurn(turnAngle);
      case LEFT -> isLeftTurn(turnAngle);
    };
  }

  /**
   * Returns if this angle represents a turn across incoming traffic.
   * <p>
   * In right hand traffic countries (US) this is a left turn. In left hand traffic (UK) countries
   * this is a right turn.
   */
  protected boolean isTurnAcrossTraffic(int turnAngle) {
    return switch (drivingDirection) {
      case RIGHT -> isLeftTurn(turnAngle);
      case LEFT -> isRightTurn(turnAngle);
    };
  }

  private double computeDrivingTraversalDuration(
    IntersectionVertex v,
    StreetEdge from,
    StreetEdge to
  ) {
    int turnAngle = calculateTurnAngle(from, to);
    if (v.trafficLight) {
      // Use constants that apply when there are stop lights.
      if (isSafeTurn(turnAngle)) {
        return getExpectedRightAtLightTimeSec();
      } else if (isTurnAcrossTraffic(turnAngle)) {
        return getExpectedLeftAtLightTimeSec();
      } else {
        return getExpectedStraightAtLightTimeSec();
      }
    } else {
      //assume highway vertex
      if (from.getCarSpeed() > 25 && to.getCarSpeed() > 25) {
        return 0;
      }

      // Use constants that apply when no stop lights.
      if (isSafeTurn(turnAngle)) {
        return getExpectedRightNoLightTimeSec();
      } else if (isTurnAcrossTraffic(turnAngle)) {
        return getExpectedLeftNoLightTimeSec();
      } else {
        return getExpectedStraightNoLightTimeSec();
      }
    }
  }

  private double computeCyclingTraversalDuration(StreetEdge from, StreetEdge to, float toSpeed) {
    var turnAngle = calculateTurnAngle(from, to);
    final var baseDuration = computeNonDrivingTraversalDuration(from, to, toSpeed);

    if (isTurnAcrossTraffic(turnAngle)) {
      return baseDuration * getAcrossTrafficBicycleTurnMultiplier();
    } else if (isSafeTurn(turnAngle)) {
      return baseDuration * getSafeBicycleTurnModifier();
    } else {
      return baseDuration;
    }
  }

  private boolean isLeftTurn(int turnAngle) {
    return turnAngle >= getMinLeftTurnAngle() && turnAngle < getMaxLeftTurnAngle();
  }

  private boolean isRightTurn(int turnAngle) {
    return turnAngle >= getMinRightTurnAngle() && turnAngle < getMaxRightTurnAngle();
  }
}
