package org.opentripplanner.routing.api.request.preference;

import static java.util.Objects.requireNonNull;
import static org.opentripplanner.utils.lang.DoubleUtils.doubleEquals;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Parameters for doing transfers between transit legs.
 *
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class TransferPreferences implements Serializable {

  public static final TransferPreferences DEFAULT = new TransferPreferences();
  private static final int MAX_NUMBER_OF_TRANSFERS = 30;

  private final Cost cost;
  private final Duration slack;
  private final double waitReluctance;
  private final int maxTransfers;
  private final int maxAdditionalTransfers;
  private final TransferOptimizationParameters optimization;
  private final Cost nonpreferredCost;

  private TransferPreferences() {
    this.cost = Cost.ZERO;
    this.slack = Duration.ofMinutes(2);
    this.waitReluctance = 1.0;
    this.maxTransfers = 12;
    this.maxAdditionalTransfers = 5;
    this.optimization = TransferOptimizationPreferences.DEFAULT;
    this.nonpreferredCost = Cost.costOfMinutes(3);
  }

  private TransferPreferences(Builder builder) {
    this.cost = builder.cost;
    this.slack = DurationUtils.requireNonNegative(builder.slack);
    this.waitReluctance = Units.reluctance(builder.waitReluctance);
    this.maxTransfers = Units.count(builder.maxTransfers, MAX_NUMBER_OF_TRANSFERS);
    this.maxAdditionalTransfers = Units.count(
      builder.maxAdditionalTransfers,
      MAX_NUMBER_OF_TRANSFERS
    );
    this.optimization = requireNonNull(builder.optimization);
    this.nonpreferredCost = builder.nonpreferredCost;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
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
    return cost.toSeconds();
  }

  /**
   * A global minimum transfer time that specifies the minimum amount of time that must
   * pass between exiting one transit vehicle and boarding another. This time is in addition to time
   * it might take to walk between transit stops, the {@link TransitPreferences#alightSlack()}, and the {@link
   * TransitPreferences#boardSlack()}.
   * This time can also be overridden by specific transfer timing information in transfers.txt
   * <p>
   * This only applies to transfer between two trips, it does not apply when boarding the first
   * transit.
   * <p>
   * Default value is 2 minutes.
   */
  public Duration slack() {
    return slack;
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

  /**
   * Ideally maxTransfers should be set in the router config, not from the client. Instead, the
   * client should use {@link #maxAdditionalTransfers)} instead to pass in the max number of
   * additional/extra transfers relative to the best trip (with the fewest possible transfers)
   * within constraint of the other search parameters. This might be too complicated to explain to
   * the customer, so you might stick to the old limit, but that has side-effects where you might
   * not find any trips on a day when a critical part of the trip is not available, because of some
   * real-time disruption.
   * <p>
   * See https://github.com/opentripplanner/OpenTripPlanner/issues/2886
   */
  public Integer maxTransfers() {
    return maxTransfers;
  }

  /**
   * The maximum number of transfers allowed in addition to the least number of transfers of any
   * result found in the search. This can be used both to improve the performance of the search, and
   * limiting the number of transfers should be done by increasing the {@link #cost} instead.
   */
  public Integer maxAdditionalTransfers() {
    return maxAdditionalTransfers;
  }

  /** Configure the transfer optimization */
  public TransferOptimizationParameters optimization() {
    return optimization;
  }

  /**
   * Penalty for using a non-preferred transfer
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
   * old functionality the same way, but we will try to map this parameter
   * so it does work similar as before.
   */
  public int nonpreferredCost() {
    return nonpreferredCost.toSeconds();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TransferPreferences that = (TransferPreferences) o;
    return (
      cost.equals(that.cost) &&
      slack.equals(that.slack) &&
      doubleEquals(that.waitReluctance, waitReluctance) &&
      maxTransfers == that.maxTransfers &&
      maxAdditionalTransfers == that.maxAdditionalTransfers &&
      optimization.equals(that.optimization) &&
      nonpreferredCost.equals(that.nonpreferredCost)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      cost,
      slack,
      waitReluctance,
      maxTransfers,
      maxAdditionalTransfers,
      optimization,
      nonpreferredCost
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TransferPreferences.class)
      .addObj("cost", cost, DEFAULT.cost)
      .addDuration("slack", slack, DEFAULT.slack)
      .addNum("waitReluctance", waitReluctance, DEFAULT.waitReluctance)
      .addNum("maxTransfers", maxTransfers, DEFAULT.maxTransfers)
      .addNum("maxAdditionalTransfers", maxAdditionalTransfers, DEFAULT.maxAdditionalTransfers)
      .addObj("optimization", optimization, DEFAULT.optimization)
      .addObj("nonpreferredCost", nonpreferredCost, DEFAULT.nonpreferredCost)
      .toString();
  }

  public static class Builder {

    private final TransferPreferences original;
    private Cost cost;
    private Duration slack;
    private Integer maxTransfers;
    private Integer maxAdditionalTransfers;
    private double waitReluctance;
    private TransferOptimizationParameters optimization;
    private Cost nonpreferredCost;

    public Builder(TransferPreferences original) {
      this.original = original;
      this.cost = original.cost;
      this.slack = original.slack;
      this.maxTransfers = original.maxTransfers;
      this.maxAdditionalTransfers = original.maxAdditionalTransfers;
      this.waitReluctance = original.waitReluctance;
      this.optimization = original.optimization;
      this.nonpreferredCost = original.nonpreferredCost;
    }

    public TransferPreferences original() {
      return original;
    }

    public Builder withCost(int cost) {
      this.cost = Cost.costOfSeconds(cost);
      return this;
    }

    public Builder withSlackSec(Number seconds) {
      return withSlack(Duration.ofSeconds(seconds.longValue()));
    }

    public Builder withSlack(Duration slack) {
      this.slack = slack;
      return this;
    }

    public Builder withNonpreferredCost(int nonpreferredCost) {
      this.nonpreferredCost = Cost.costOfSeconds(nonpreferredCost);
      return this;
    }

    public Builder withWaitReluctance(double waitReluctance) {
      this.waitReluctance = waitReluctance;
      return this;
    }

    public Builder withOptimization(TransferOptimizationParameters optimization) {
      this.optimization = optimization;
      return this;
    }

    public Builder withMaxTransfers(Integer maxTransfers) {
      this.maxTransfers = maxTransfers;
      return this;
    }

    public Builder withMaxAdditionalTransfers(Integer maxAdditionalTransfers) {
      this.maxAdditionalTransfers = maxAdditionalTransfers;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    TransferPreferences build() {
      var value = new TransferPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
