package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;

// TODO VIA: Javadoc
public class TransferPreferences implements Cloneable, Serializable {

  private int cost = 0;
  private int slack = 120;
  private int nonpreferredCost = 180;
  private double waitReluctance = 1.0;

  @Deprecated
  private double waitAtBeginningFactor = 0.4;

  private TransferOptimizationParameters optimization = TransferOptimizationPreferences.DEFAULT;
  private Integer maxTransfers = 12;

  public TransferPreferences() {}

  public TransferPreferences(TransferPreferences other) {
    this.cost = other.cost;
    this.slack = other.slack;
    this.nonpreferredCost = other.nonpreferredCost;
    this.waitReluctance = other.waitReluctance;
    this.waitAtBeginningFactor = other.waitAtBeginningFactor;
    this.optimization = other.optimization;
    this.maxTransfers = other.maxTransfers;
  }

  /**
   * An extra penalty added on transfers (i.e. all boardings except the first one). Not to be
   * confused with bikeBoardCost and walkBoardCost, which are the cost of boarding a vehicle with
   * and without a bicycle. The boardCosts are used to model the 'usual' perceived cost of using a
   * transit vehicle, and the transferCost is used when a user requests even less transfers. In the
   * latter case, we don't actually optimize for fewest transfers, as this can lead to absurd
   * results. Consider a trip in New York from Grand Army Plaza (the one in Brooklyn) to Kalustyan's
   * at noon. The true lowest transfers route is to wait until midnight, when the 4 train runs local
   * the whole way. The actual fastest route is the 2/3 to the 4/5 at Nevins to the 6 at Union
   * Square, which takes half an hour. Even someone optimizing for fewest transfers doesn't want to
   * wait until midnight. Maybe they would be willing to walk to 7th Ave and take the Q to Union
   * Square, then transfer to the 6. If this takes less than optimize_transfer_penalty seconds, then
   * that's what we'll return.
   */
  public int cost() {
    return cost;
  }

  public void setCost(int cost) {
    this.cost = cost;
  }

  /**
   * A global minimum transfer time (in seconds) that specifies the minimum amount of time that must
   * pass between exiting one transit vehicle and boarding another. This time is in addition to time
   * it might take to walk between transit stops, the {@link TransitPreferences#alightSlack()}, and the {@link
   * TransitPreferences#boardSlack()}. This time should also be overridden by specific transfer timing information in
   * transfers.txt
   * <p>
   * This only apply to transfers between two trips, it does not apply when boarding the first
   * transit.
   * <p>
   * Unit is seconds. Default value is 2 minutes.
   */
  public int slack() {
    return slack;
  }

  public void setSlack(int slack) {
    this.slack = slack;
  }

  /**
   * Penalty for using a non-preferred transfer
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
   * old functionality the same way, but we will try to map this parameter
   * so it does work similar as before.
   */
  public int nonpreferredCost() {
    return nonpreferredCost;
  }

  public void setNonpreferredCost(int nonpreferredCost) {
    this.nonpreferredCost = nonpreferredCost;
  }

  /**
   * How much worse is waiting for a transit vehicle than being on a transit vehicle, as a
   * multiplier. The default value treats wait and on-vehicle time as the same.
   * <p>
   * It may be tempting to set this higher than walkReluctance (as studies often find this kind of
   * preferences among riders) but the planner will take this literally and walk down a transit line
   * to avoid waiting at a stop. This used to be set less than 1 (0.95) which would make waiting
   * offboard preferable to waiting onboard in an interlined trip. That is also undesirable.
   * <p>
   * If we only tried the shortest possible transfer at each stop to neighboring stop patterns, this
   * problem could disappear.
   */
  public double waitReluctance() {
    return waitReluctance;
  }

  public void setWaitReluctance(double waitReluctance) {
    this.waitReluctance = waitReluctance;
  }

  public void setWaitAtBeginningFactor(double waitAtBeginningFactor) {
    this.waitAtBeginningFactor = waitAtBeginningFactor;
  }

  /**
   * How much less bad is waiting at the beginning of the trip (replaces waitReluctance on the first
   * boarding)
   *
   * @deprecated TODO OTP2 Probably a regression, but I'm not sure it worked correctly in OTP 1.X
   * either. It could be a part of itinerary-filtering after a Raptor search.
   */
  public double waitAtBeginningFactor() {
    return waitAtBeginningFactor;
  }

  /** Configure the transfer optimization */
  public TransferOptimizationParameters optimization() {
    return optimization;
  }

  public void setOptimization(TransferOptimizationParameters optimization) {
    this.optimization = optimization;
  }

  /**
   * Ideally maxTransfers should be set in the router config, not here. Instead the client should be
   * able to pass in a parameter for the max number of additional/extra transfers relative to the
   * best trip (with the fewest possible transfers) within constraint of the other search
   * parameters(TODO OTP2 Expose {@link org.opentripplanner.transit.raptor.api.request.SearchParams#numberOfAdditionalTransfers()}
   * in APIs). This might be to complicated to explain to the customer, so we might stick to the old
   * limit, but that have side-effects that you might not find any trips on a day where a critical
   * part of the trip is not available, because of some real-time disruption.
   * <p>
   * See https://github.com/opentripplanner/OpenTripPlanner/issues/2886
   */
  public Integer maxTransfers() {
    return maxTransfers;
  }

  public void setMaxTransfers(Integer maxTransfers) {
    this.maxTransfers = maxTransfers;
  }

  public TransferPreferences clone() {
    try {
      // TODO VIA (Thomas): 2022-08-26 not cloning TransferOptimizationParameters (that's how it was before)

      var clone = (TransferPreferences) super.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
