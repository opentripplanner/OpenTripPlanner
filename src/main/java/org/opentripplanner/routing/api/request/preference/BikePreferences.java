package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO VIA (Thomas): Javadoc
public class BikePreferences implements Cloneable, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(BikePreferences.class);

  private BicycleOptimizeType optimizeType = BicycleOptimizeType.SAFE;

  private double speed = 5;

  // TODO VIA (Thomas): Is this part of transit preferences
  private int boardCost = 60 * 10;
  private double walkingSpeed = 1.33;
  private double walkingReluctance = 5.0;
  private double reluctance = 2.0;

  private int switchTime;
  private int switchCost;
  private int parkTime = 60;
  /** Cost of parking a bike. */
  private int parkCost = 120;

  private TimeSlopeSafetyTriangle optimizeTriangle = TimeSlopeSafetyTriangle.DEFAULT;

  public BikePreferences clone() {
    try {
      return (BikePreferences) super.clone();
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  /**
   * The set of characteristics that the user wants to optimize for -- defaults to SAFE.
   */
  public BicycleOptimizeType optimizeType() {
    return optimizeType;
  }

  public void setOptimizeType(BicycleOptimizeType optimizeType) {
    this.optimizeType = optimizeType;
  }

  public TimeSlopeSafetyTriangle optimizeTriangle() {
    return optimizeTriangle;
  }

  /**
   * Sets the bicycle optimize triangle routing parameters. See {@link TimeSlopeSafetyTriangle}
   * for details.
   */
  public void initOptimizeTriangle(double time, double slope, double safety) {
    this.optimizeTriangle = new TimeSlopeSafetyTriangle(time, slope, safety);
  }

  /**
   * Default: 5 m/s, ~11 mph, a random bicycling speed
   */
  public double speed() {
    return speed;
  }

  public void setSpeed(double speed) {
    this.speed = speed;
  }

  /**
   * Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot. This
   * is in addition to the cost of the transfer(biking) and waiting-time. It is also in addition to
   * the {@link TransferPreferences#cost()}.
   */
  public int boardCost() {
    return boardCost;
  }

  public void setBoardCost(int boardCost) {
    this.boardCost = boardCost;
  }

  /**
   * The walking speed when walking a bike. Default: 1.33 m/s ~ Same as walkSpeed
   */
  public double walkingSpeed() {
    return walkingSpeed;
  }

  public void setWalkingSpeed(double walkingSpeed) {
    this.walkingSpeed = walkingSpeed;
  }

  /**
   * A multiplier for how bad walking is, compared to being in transit for equal
   * lengths of time. Empirically, values between 2 and 4 seem to correspond
   * well to the concept of not wanting to walk too much without asking for
   * totally ridiculous itineraries, but this observation should in no way be
   * taken as scientific or definitive. Your mileage may vary. See
   * https://github.com/opentripplanner/OpenTripPlanner/issues/4090 for impact on
   * performance with high values. Default value: 2.0
   */
  public double walkingReluctance() {
    return walkingReluctance;
  }

  public void setWalkingReluctance(double walkingReluctance) {
    this.walkingReluctance = walkingReluctance;
  }

  public void setReluctance(double reluctance) {
    this.reluctance = reluctance;
  }

  public double reluctance() {
    return reluctance;
  }

  /** Time to get on and off your own bike */
  public int switchTime() {
    return switchTime;
  }

  public void setSwitchTime(int switchTime) {
    this.switchTime = switchTime;
  }

  /** Cost of getting on and off your own bike */
  public int switchCost() {
    return switchCost;
  }

  public void setSwitchCost(int switchCost) {
    this.switchCost = switchCost;
  }

  /** Time to park a bike */
  public int parkTime() {
    return parkTime;
  }

  public void setParkTime(int parkTime) {
    this.parkTime = parkTime;
  }

  public void setParkCost(int parkCost) {
    this.parkCost = parkCost;
  }

  public int parkCost() {
    return parkCost;
  }
}
