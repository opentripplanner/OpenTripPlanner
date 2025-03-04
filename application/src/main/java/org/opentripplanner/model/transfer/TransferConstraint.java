package org.opentripplanner.model.transfer;

import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.NOT_ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.PREFERRED;
import static org.opentripplanner.model.transfer.TransferPriority.RECOMMENDED;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class holds transfer constraint information.
 * <p>
 * The class is immutable.
 */
public class TransferConstraint implements Serializable, RaptorTransferConstraint {

  /**
   * A regular transfer is a transfer with no constraints.
   */
  public static final TransferConstraint REGULAR_TRANSFER = of().build();

  /**
   * STAY_SEATED is not a priority, but we assign a cost to it to be able to compare it with other
   * transfers with a priority and the {@link #GUARANTEED_TRANSFER_COST}.
   */
  private static final int STAY_SEATED_TRANSFER_COST = 10_00;

  /**
   * GUARANTEED is not a priority, but we assign a cost to it to be able to compare it with other
   * transfers with a priority. The cost is better than a pure prioritized transfer, but the
   * priority and GUARANTEED attribute is added together; Hence a (GUARANTEED, RECOMMENDED) transfer
   * is better than (GUARANTEED, ALLOWED).
   */
  private static final int GUARANTEED_TRANSFER_COST = 20_00;

  /**
   * A cost penalty of 10 points is added to a Transfer which is NOT stay-seated or guaranteed. This
   * makes sure that stay-seated and guaranteed transfers take precedence over the priority cost.
   */
  private static final int NONE_FACILITATED_COST = 30_00;

  /**
   * A cost penalty of 4 points is added to transfers which are NOT stay-seated or guaranteed.
   */
  private static final int DEFAULT_COST = NONE_FACILITATED_COST + ALLOWED.cost();

  /**
   * Starting point for calculating the transfer constraint cost.
   */
  public static final int ZERO_COST = 0;

  /**
   * Used with {@link #getMaxWaitTime()} and {@link #getMinTransferTime()} to indicate the parameter
   * is not available.
   */
  public static final int NOT_SET = -1;

  private final TransferPriority priority;

  private final boolean staySeated;

  private final boolean guaranteed;

  private final int maxWaitTime;

  private final int minTransferTime;

  private final boolean includeInRaptorRouting;

  private TransferConstraint(Builder builder) {
    this.priority = builder.priority;
    this.staySeated = builder.staySeated;
    this.guaranteed = builder.guaranteed;
    this.maxWaitTime = builder.maxWaitTime;
    this.minTransferTime = builder.minTransferTime;

    // Decide if the transfer needs to be taken into account in the Raptor routing process
    // or can be dealt with outside raptor, e.g in path transfer optimization.
    this.includeInRaptorRouting =
      staySeated || guaranteed || priority == NOT_ALLOWED || minTransferTime != NOT_SET;

    if (isMaxWaitTimeSet() && !guaranteed) {
      throw new IllegalArgumentException("'maxWaitTime' do only apply to guaranteed transfers.");
    }
  }

  /**
   * Calculate a cost for prioritizing transfers in a path, to select the best path with respect to
   * transfers. This cost is not related in any way to the path generalized-cost. It takes only the
   * transfer constraint attributes in consideration.
   * <p>
   * When comparing paths that ride the same trips, this can be used to find the optimal places to
   * do the transfers. The cost is created to prioritize the following:
   * <ol>
   *     <li>{@code stay-seated} - cost: 10 points</li>
   *     <li>{@code guaranteed} - cost: 20 points</li>
   *     <li>None facilitated  - cost: 30 points</li>
   * </ol>
   * In addition, the {@code priority} cost is added. See {@link TransferPriority#cost()}.
   *
   * @param c The transfer to return a cost for, or {@code null} if the transfer is a regular OSM
   *          street generated transfer.
   */
  public static int cost(@Nullable TransferConstraint c) {
    return c == null ? DEFAULT_COST : c.cost();
  }

  public static Builder of() {
    return new Builder();
  }

  /**
   * @see #cost(TransferConstraint)
   */
  public int cost() {
    return priority.cost() + facilitatedCost();
  }

  public TransferPriority getPriority() {
    return priority;
  }

  public boolean isGuaranteed() {
    return guaranteed;
  }

  /**
   * A facilitated transfer is allowed even if there might not be enough time to walk or if the
   * alight-slack or board-slack is too tight. We ignore slack for facilitated transfers.
   * <p>
   * This is an aggregated field, which encapsulates an OTP specific rule. A facilitated transfer is
   * either stay-seated or guaranteed. High priority transfers are not facilitated.
   */
  public boolean isFacilitated() {
    return staySeated || guaranteed;
  }

  /**
   * This switch enables transfers in Raptor, ignoring transfer constraints with for example only
   * priority set.
   */
  public boolean includeInRaptorRouting() {
    return includeInRaptorRouting;
  }

  @Override
  public boolean isNotAllowed() {
    return priority == NOT_ALLOWED;
  }

  @Override
  public boolean isRegularTransfer() {
    // Note! The 'maxWaitTime' is only valid with the guaranteed flag set, so we
    // do not need to check it here
    return !(isFacilitated() || isMinTransferTimeSet() || priority.isConstrained());
  }

  /**
   * Also known as interlining of GTFS trips with the same block id.
   */
  public boolean isStaySeated() {
    return staySeated;
  }

  /**
   * Maximum time after scheduled departure time the connecting transport is guaranteed to wait for
   * the delayed trip.
   * <p>
   * THIS IS NOT CONSIDERED IN RAPTOR. OTP relies on real-time data for this, so if the "from"
   * vehicle is delayed, then the real time system is also responsible for propagating the delay
   * onto the "to" trip.
   */
  public int getMaxWaitTime() {
    return maxWaitTime;
  }

  /**
   * The min-transfer-time specify lower bound for the transfer time. {@link
   * org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.ConstrainedBoardingSearch}
   * uses this to make sure at least the amount of seconds specified is available to do the
   * transfer. If the path transfer takes more time than specified by the {@code min-transfer-time}
   * then the path transfer is used. Normal slack parameters are added to the path transfer, but not
   * to the {@code min-transfer-time}.
   */
  public int getMinTransferTime() {
    return minTransferTime;
  }

  public boolean isMinTransferTimeSet() {
    return minTransferTime != NOT_SET;
  }

  /**
   * This method is used to calculate the earliest-board-time in a forward search, and the
   * latest-alight-time in a reverse search for a constrained transfer. If the given transfer
   * constraint is a regular transfer, we use the given {@code regularTransferEBT} function
   * to calculate the targetTime.
   * <p>
   * In a forward search we ADD({@code timeAddOp}) the transfer time to the given
   * {@code sourceTransitArrivalTime} to find the earliest-board-time for the target trip.
   * <p>
   * In a reverse search we SUBTRACT({@code timeAddOp}) the transfer time from the given
   * {@code sourceTransitArrivalTime} to find the latest-alight-time for the target trip.
   */
  public int calculateTransferTargetTime(
    int sourceTransitArrivalTime,
    int transferSlack,
    IntSupplier calcRegularTransferTargetTime,
    SearchDirection direction
  ) {
    // Ignore slack and walking-time for guaranteed and stay-seated transfers
    if (isFacilitated()) {
      return sourceTransitArrivalTime;
    }

    // Ignore transfer, board and alight slack for min-transfer-time
    if (isMinTransferTimeSet()) {
      int minTransferTime = getMinTransferTime() + transferSlack;
      if (direction.isForward()) {
        int minTransferBoardTime = sourceTransitArrivalTime + minTransferTime;
        return OTPFeature.MinimumTransferTimeIsDefinitive.isOn()
          ? minTransferBoardTime
          : Math.max(minTransferBoardTime, calcRegularTransferTargetTime.getAsInt());
      } else {
        int minTransferBoardTime = sourceTransitArrivalTime - minTransferTime;
        return OTPFeature.MinimumTransferTimeIsDefinitive.isOn()
          ? minTransferBoardTime
          : Math.min(minTransferBoardTime, calcRegularTransferTargetTime.getAsInt());
      }
    }
    // Transfers with priority only apply to the cost not the transfer time
    return calcRegularTransferTargetTime.getAsInt();
  }

  @Override
  public int hashCode() {
    return Objects.hash(priority, staySeated, guaranteed, maxWaitTime);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final TransferConstraint that)) {
      return false;
    }
    return (
      staySeated == that.staySeated &&
      guaranteed == that.guaranteed &&
      priority == that.priority &&
      maxWaitTime == that.maxWaitTime
    );
  }

  public String toString() {
    if (isRegularTransfer()) {
      return "{no constraints}";
    }

    return ToStringBuilder.of()
      .addEnum("priority", priority, ALLOWED)
      .addBoolIfTrue("staySeated", staySeated)
      .addBoolIfTrue("guaranteed", guaranteed)
      .addDurationSec("minTransferTime", minTransferTime, NOT_SET)
      .addDurationSec("maxWaitTime", maxWaitTime, NOT_SET)
      .toString();
  }

  /**
   * Return a cost for stay-seated, guaranteed or none-facilitated transfers. This is used to
   * prioritize stay-seated over guaranteed, and guaranteed over non-facilitated transfers.
   */
  private int facilitatedCost() {
    if (staySeated) {
      return STAY_SEATED_TRANSFER_COST;
    }
    if (guaranteed) {
      return GUARANTEED_TRANSFER_COST;
    }
    return NONE_FACILITATED_COST;
  }

  private boolean isMaxWaitTimeSet() {
    return maxWaitTime != NOT_SET;
  }

  public static class Builder {

    private TransferPriority priority = ALLOWED;
    private boolean staySeated = false;
    private boolean guaranteed = false;
    private int maxWaitTime = NOT_SET;
    private int minTransferTime = NOT_SET;

    public Builder priority(TransferPriority priority) {
      this.priority = priority;
      return this;
    }

    public Builder notAllowed() {
      return priority(NOT_ALLOWED);
    }

    public Builder recommended() {
      return priority(RECOMMENDED);
    }

    public Builder preferred() {
      return priority(PREFERRED);
    }

    public Builder staySeated(boolean enable) {
      this.staySeated = enable;
      return this;
    }

    public Builder staySeated() {
      return staySeated(true);
    }

    public Builder guaranteed(boolean enable) {
      this.guaranteed = enable;
      return this;
    }

    public Builder guaranteed() {
      return guaranteed(true);
    }

    public Builder maxWaitTime(int maxWaitTime) {
      this.maxWaitTime = maxWaitTime;
      return this;
    }

    public Builder minTransferTime(int minTransferTime) {
      this.minTransferTime = minTransferTime;
      return this;
    }

    public TransferConstraint build() {
      return new TransferConstraint(this);
    }
  }
}
