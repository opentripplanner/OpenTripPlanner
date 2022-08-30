package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;

public class WalkPreferences implements Cloneable, Serializable {

  /**
   * Human walk speed along streets, in meters per second.
   * <p>
   * Default: 1.33 m/s ~ 3mph, <a href="http://en.wikipedia.org/wiki/Walking">avg. human walk
   * speed</a>
   */
  private double speed = 1.33;
  /**
   * A multiplier for how bad walking is, compared to being in transit for equal
   * lengths of time. Empirically, values between 2 and 4 seem to correspond
   * well to the concept of not wanting to walk too much without asking for
   * totally ridiculous itineraries, but this observation should in no way be
   * taken as scientific or definitive. Your mileage may vary. See
   * https://github.com/opentripplanner/OpenTripPlanner/issues/4090 for impact on
   * performance with high values. Default value: 2.0
   */
  private double reluctance = 2.0;

  /**
   * This prevents unnecessary transfers by adding a cost for boarding a vehicle. This is in
   * addition to the cost of the transfer(walking) and waiting-time. It is also in addition to the
   * {@link TransferPreferences#cost()}.
   */
  private int boardCost = 60 * 10;
  /** Used instead of walk reluctance for stairs */
  private double stairsReluctance = 2.0;
  /**
   * How much more time does it take to walk a flight of stairs compared to walking a similar
   * horizontal length
   * <p>
   * Default value is based on: Fujiyama, T., & Tyler, N. (2010). Predicting the walking speed of
   * pedestrians on stairs. Transportation Planning and Technology, 33(2), 177–202.
   */
  private double stairsTimeFactor = 3.0;

  /**
   * Factor for how much the walk safety is considered in routing. Value should be between 0 and 1.
   * If the value is set to be 0, safety is ignored.
   */
  private double safetyFactor = 1.0;

  public WalkPreferences clone() {
    try {
      var clone = (WalkPreferences) super.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public void setSpeed(double speed) {
    this.speed = speed;
  }

  public double speed() {
    return speed;
  }

  public void setReluctance(double reluctance) {
    this.reluctance = reluctance;
  }

  public double reluctance() {
    return reluctance;
  }

  public void setBoardCost(int boardCost) {
    this.boardCost = boardCost;
  }

  public int boardCost() {
    return boardCost;
  }

  public void setStairsReluctance(double stairsReluctance) {
    this.stairsReluctance = stairsReluctance;
  }

  public double stairsReluctance() {
    return stairsReluctance;
  }

  public void setStairsTimeFactor(double stairsTimeFactor) {
    this.stairsTimeFactor = stairsTimeFactor;
  }

  public double stairsTimeFactor() {
    return stairsTimeFactor;
  }

  public void setSafetyFactor(double walkSafetyFactor) {
    if (walkSafetyFactor < 0) {
      this.safetyFactor = 0;
    } else if (walkSafetyFactor > 1) {
      this.safetyFactor = 1;
    } else {
      this.safetyFactor = walkSafetyFactor;
    }
  }

  public double safetyFactor() {
    return safetyFactor;
  }
}
