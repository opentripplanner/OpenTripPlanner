package org.opentripplanner.model.transfer;

import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.NOT_ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.PREFERRED;
import static org.opentripplanner.model.transfer.TransferPriority.RECOMMENDED;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;

/**
 * This class holds transfer constraint information.
 * <p>
 * The class is immutable.
 */
public class TransferConstraint implements Serializable, RaptorTransferConstraint {

    private static final long serialVersionUID = 1L;

    /**
     * STAY_SEATED is not a priority, but we assign a cost to it to be able to compare it with other
     * transfers with a priority and the {@link #GUARANTIED_TRANSFER_COST}.
     */
    private static final int STAY_SEATED_TRANSFER_COST = 10_00;

    /**
     * GUARANTIED is not a priority, but we assign a cost to it to be able to compare it with other
     * transfers with a priority. The cost is better than a pure prioritized transfer, but the
     * priority and GUARANTIED attribute is added together; Hence a (GUARANTIED, RECOMMENDED)
     * transfer is better than (GUARANTIED, ALLOWED).
     */
    private static final int GUARANTIED_TRANSFER_COST = 20_00;

    /**
     * A cost penalty of 10 points is added to a Transfer which is NOT stay-seated or guaranteed.
     * This makes sure that stay-seated and guaranteed transfers take precedence over the priority
     * cost.
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
     * Regular street transfers should be given this cost.
     */
    public static final int MAX_WAIT_TIME_NOT_SET = -1;


    private final TransferPriority priority;

    private final boolean staySeated;

    private final boolean guaranteed;

    private final int maxWaitTime;


    private TransferConstraint(Builder builder) {
        this.priority = builder.priority;
        this.staySeated = builder.staySeated;
        this.guaranteed = builder.guaranteed;
        this.maxWaitTime = builder.maxWaitTime;

        if(isMaxWaitTimeSet() && !guaranteed) {
            throw new IllegalArgumentException(
                    "'maxWaitTime' do only apply to guaranteed transfers."
            );
        }
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

    public boolean isStaySeated() {
        return staySeated;
    }

    public boolean isGuaranteed() {
        return guaranteed;
    }

    /**
     * A facilitated transfer is allowed even if there might not be enough time to walk or
     * if the alight-slack or board-slack is too tight. We ignore slack for facilitated transfers.
     * <p>
     * This is an aggregated field, which encapsulates an OTP specific rule. A facilitated transfer
     * is either stay-seated or guaranteed. High priority transfers are not.
     */
    public boolean isFacilitated() {
        return staySeated || guaranteed;
    }

    /**
     * Maximum time after scheduled departure time the connecting transport is guarantied to wait
     * for the delayed trip.
     */
    public int getMaxWaitTime() {
        return maxWaitTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, staySeated, guaranteed, maxWaitTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof TransferConstraint)) { return false; }
        final TransferConstraint that = (TransferConstraint) o;
        return staySeated == that.staySeated
                && guaranteed == that.guaranteed
                && priority == that.priority
                && maxWaitTime == that.maxWaitTime;
    }

    public String toString() {
        if(noConstraints()) { return "{no constraints}"; }

        return ToStringBuilder.of()
                .addEnum("priority", priority, ALLOWED)
                .addBoolIfTrue("staySeated", staySeated)
                .addBoolIfTrue("guaranteed", guaranteed)
                .addDurationSec("maxWaitTime", maxWaitTime, MAX_WAIT_TIME_NOT_SET)
                .toString();
    }

    public boolean noConstraints() {
        // Note! The 'maxWaitTime' is only valid with the guaranteed flag set, so we
        // do not need to check it here
        return !(staySeated || guaranteed || priority.isConstrained());
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

    /**
     * Return a cost for stay-seated, guaranteed or none-facilitated transfers. This is
     * used to prioritize stay-seated over guaranteed, and guaranteed over non-facilitated
     * transfers.
     */
    private int facilitatedCost() {
        if(staySeated) { return STAY_SEATED_TRANSFER_COST; }
        if(guaranteed) { return GUARANTIED_TRANSFER_COST; }
        return NONE_FACILITATED_COST;
    }



    public static Builder create() { return new Builder(); }

    private boolean isMaxWaitTimeSet() {
        return maxWaitTime != MAX_WAIT_TIME_NOT_SET;
    }

    public static class Builder {
        private TransferPriority priority = ALLOWED;
        private boolean staySeated = false;
        private boolean guaranteed = false;
        private int maxWaitTime = MAX_WAIT_TIME_NOT_SET;

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

        public TransferConstraint build() {
            return new TransferConstraint(this);
        }
    }
}
