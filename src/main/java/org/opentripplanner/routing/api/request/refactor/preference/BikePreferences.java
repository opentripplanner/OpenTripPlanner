package org.opentripplanner.routing.api.request.refactor.preference;

import org.opentripplanner.routing.core.BicycleOptimizeType;

public class BikePreferences {

  /**
   * The set of characteristics that the user wants to optimize for -- defaults to SAFE.
   */
  private BicycleOptimizeType optimizeType = BicycleOptimizeType.SAFE;

  /**
   * Default: 5 m/s, ~11 mph, a random bicycling speed
   */
  private double speed = 5;

  // TODO: 2022-08-17 check documentation
  /**
   * Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot. This
   * is in addition to the cost of the transfer(biking) and waiting-time. It is also in addition to
   * the {@link #transferCost}.
   */
  private int boardCost = 60 * 10;
  /**
   * Default: 1.33 m/s ~ Same as walkSpeed
   */
  private double walkingSpeed = 1.33;
  /**
   * A multiplier for how bad walking is, compared to being in transit for equal
   * lengths of time. Empirically, values between 2 and 4 seem to correspond
   * well to the concept of not wanting to walk too much without asking for
   * totally ridiculous itineraries, but this observation should in no way be
   * taken as scientific or definitive. Your mileage may vary. See
   * https://github.com/opentripplanner/OpenTripPlanner/issues/4090 for impact on
   * performance with high values. Default value: 2.0
   */
  private double walkingReluctance = 5.0;
  private double reluctance = 2.0;

  /** Time to get on and off your own bike */
  private int switchTime;
  /** Cost of getting on and off your own bike */
  private int switchCost;
  /** Time to park a bike */
  private int parkTime = 60;
  /** Cost of parking a bike. */
  private int parkCost = 120;
  /**
   * For the bike triangle, how important time is. triangleTimeFactor+triangleSlopeFactor+triangleSafetyFactor
   * == 1
   */
  private double triangleTimeFactor;
  /** For the bike triangle, how important slope is */
  private double triangleSlopeFactor;
  /** For the bike triangle, how important safety is */
  private double triangleSafetyFactor;

  // TODO: 2022-08-18 Is this right?
  private boolean parkAndRide = false;

  public BicycleOptimizeType optimizeType() {
    return optimizeType;
  }

  public void setSpeed(double speed) {
    this.speed = speed;
  }

  public double speed() {
    return speed;
  }

  public int boardCost() {
    return boardCost;
  }

  public double walkingSpeed() {
    return walkingSpeed;
  }

  public double walkingReluctance() {
    return walkingReluctance;
  }

  public double reluctance() {
    return reluctance;
  }

  public void setSwitchTime(int switchTime) {
    this.switchTime = switchTime;
  }

  public int switchTime() {
    return switchTime;
  }

  public void setSwitchCost(int switchCost) {
    this.switchCost = switchCost;
  }

  public int switchCost() {
    return switchCost;
  }

  public int parkTime() {
    return parkTime;
  }

  public int parkCost() {
    return parkCost;
  }

  public void setTriangleTimeFactor(double triangleTimeFactor) {
    this.triangleTimeFactor = triangleTimeFactor;
  }

  public double triangleTimeFactor() {
    return triangleTimeFactor;
  }

  public void setTriangleSlopeFactor(double triangleSlopeFactor) {
    this.triangleSlopeFactor = triangleSlopeFactor;
  }

  public double triangleSlopeFactor() {
    return triangleSlopeFactor;
  }

  public void setTriangleSafetyFactor(double triangleSafetyFactor) {
    this.triangleSafetyFactor = triangleSafetyFactor;
  }

  public double triangleSafetyFactor() {
    return triangleSafetyFactor;
  }

  public void setParkAndRide(boolean parkAndRide) {
    this.parkAndRide = parkAndRide;
  }

  public boolean parkAndRide() {
    return parkAndRide;
  }
}
