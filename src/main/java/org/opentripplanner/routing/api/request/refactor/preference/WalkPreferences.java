package org.opentripplanner.routing.api.request.refactor.preference;

public class WalkPreferences {
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
  // TODO: 2022-08-18 fix documentation
  /**
   * This prevents unnecessary transfers by adding a cost for boarding a vehicle. This is in
   * addition to the cost of the transfer(walking) and waiting-time. It is also in addition to the
   * {@link #transferCost}.
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

  public double speed() {
    return speed;
  }

  public double reluctance() {
    return reluctance;
  }

  public int boardCost() {
    return boardCost;
  }

  public double stairsReluctance() {
    return stairsReluctance;
  }

  public double stairsTimeFactor() {
    return stairsTimeFactor;
  }
}
