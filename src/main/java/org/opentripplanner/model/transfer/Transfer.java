/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.transfer;

import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;

public final class Transfer implements Serializable {

    /**
     * Regular street transfers should be given this cost.
     */
    public static final int NEUTRAL_TRANSFER_COST = 0;

    /**
     * Regular street transfers should be given this cost.
     */
    public static final int MAX_WAIT_TIME_NOT_SET = -1;

    private static final long serialVersionUID = 1L;

    private final TransferPoint from;

    private final TransferPoint to;

    private final TransferPriority priority;

    private final boolean staySeated;

    private final boolean guaranteed;

    private final int maxWaitTime;

    public Transfer(
            TransferPoint from,
            TransferPoint to,
            TransferPriority priority,
            boolean staySeated,
            boolean guaranteed,
            int maxWaitTime
    ) {
        this.from = from;
        this.to = to;
        this.priority = priority;
        this.staySeated = staySeated;
        this.guaranteed = guaranteed;
        this.maxWaitTime = maxWaitTime;
    }

    /**
     * Calculate a cost for prioritizing transfers in a path to select the best path with respect to
     * transfers. This cost should not be mixed with the path generalized-cost.
     *
     * @param t The transfer to return a cost for, or {@code null} if the transfer is a regular OSM
     *          street generated transfer.
     * @see TransferPriority#cost(boolean, boolean)
     */
    public static int priorityCost(@Nullable Transfer t) {
        return t == null ? NEUTRAL_TRANSFER_COST : t.priority.cost(t.staySeated, t.guaranteed);
    }

    public TransferPoint getFrom() {
        return from;
    }

    public TransferPoint getTo() {
        return to;
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
     * Maximum time after scheduled departure time the connecting transport is guarantied to wait
     * for the delayed trip.
     */
    public int getMaxWaitTime() {
        return maxWaitTime;
    }

    /**
     * <a href="https://developers.google.com/transit/gtfs/reference/gtfs-extensions#specificity-of-a-transfer">
     * Specificity of a transfer
     * </a>
     */
    public int getSpecificityRanking() {
        return from.getSpecificityRanking() + to.getSpecificityRanking();
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, priority, staySeated, guaranteed);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Transfer)) { return false; }
        final Transfer transfer = (Transfer) o;
        return staySeated == transfer.staySeated
                && guaranteed == transfer.guaranteed
                && priority == transfer.priority
                && Objects.equals(from, transfer.from)
                && Objects.equals(to, transfer.to);
    }

    public String toString() {
        return ToStringBuilder.of(Transfer.class)
                .addObj("from", from)
                .addObj("to", to)
                .addEnum("priority", priority, ALLOWED)
                .addDurationSec("maxWaitTime", maxWaitTime, MAX_WAIT_TIME_NOT_SET)
                .addBoolIfTrue("staySeated", staySeated)
                .addBoolIfTrue("guaranteed", guaranteed)
                .toString();
    }

    public boolean noConstraints() {
        boolean prioritySet = priority != ALLOWED;
        boolean maxWaitTimeSet = maxWaitTime != MAX_WAIT_TIME_NOT_SET;
        return !(staySeated || guaranteed || prioritySet || maxWaitTimeSet);
    }
}
