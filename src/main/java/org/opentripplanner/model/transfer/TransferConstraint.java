package org.opentripplanner.model.transfer;

import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;

public class TransferConstraint implements Serializable, RaptorTransferConstraint {

    private static final long serialVersionUID = 1L;

    /**
     * Regular street transfers should be given this cost.
     */
    public static final int MAX_WAIT_TIME_NOT_SET = -1;


    private final TransferPriority priority;

    private final boolean staySeated;

    private final boolean guaranteed;

    private final int maxWaitTime;

    public TransferConstraint(
            TransferPriority priority,
            boolean staySeated,
            boolean guaranteed,
            int maxWaitTime
    ) {
        this.priority = priority;
        this.staySeated = staySeated;
        this.guaranteed = guaranteed;
        this.maxWaitTime = maxWaitTime;
    }

    /**
     * Calculate a cost for prioritizing transfers in a path to select the best path with respect to
     * transfers. This cost is not related in any way to the path generalized-cost.
     */
    public int priorityCost() {
        return priority.cost(staySeated, guaranteed);
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
        boolean prioritySet = priority != ALLOWED;
        boolean maxWaitTimeSet = maxWaitTime != MAX_WAIT_TIME_NOT_SET;
        return !(staySeated || guaranteed || prioritySet || maxWaitTimeSet);
    }

    /**
     * Calculate a cost for prioritizing transfers in a path to select the best path with respect to
     * transfers. This cost is not related in any way to the path generalized-cost.
     *
     * @param c The transfer to return a cost for, or {@code null} if the transfer is a regular OSM
     *          street generated transfer.
     * @see TransferPriority#cost(boolean, boolean)
     */
    public static int priorityCost(@Nullable TransferConstraint c) {
        return c == null ? TransferPriority.NEUTRAL_PRIORITY_COST : c.priorityCost();
    }

}
