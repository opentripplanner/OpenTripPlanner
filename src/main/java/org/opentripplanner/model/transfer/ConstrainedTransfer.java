/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.transfer;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;


/**
 * A constrained transfer is a transfer witch is restricted in one ore more ways by the transit data
 * provider. It can be guaranteed or stay-seated, have a priority (NOT_ALLOWED, ALLOWED,
 * RECOMMENDED, PREFERRED) or some sort of time constraint attached to it. It is applied to a
 * transfer from a transfer-point to another point. A transfer point is a combination of stop and
 * route/trip.
 */
public final class ConstrainedTransfer implements Serializable {

    private static final long serialVersionUID = 1L;

    private final TransferPoint from;

    private final TransferPoint to;

    private final TransferConstraint constraint;

    public ConstrainedTransfer(
            TransferPoint from,
            TransferPoint to,
            TransferConstraint constraint
    ) {
        this.from = from;
        this.to = to;
        this.constraint = constraint;
    }

    /**
     * Calculate a cost for prioritizing transfers in a path to select the best path with respect to
     * transfers. This cost is not related in any way to the path generalized-cost.
     *
     * @param t The transfer to return a cost for, or {@code null} if the transfer is a regular OSM
     *          street generated transfer.
     * @see TransferPriority#cost(boolean, boolean)
     */
    public static int priorityCost(@Nullable ConstrainedTransfer t) {
        return t==null ? TransferPriority.NEUTRAL_PRIORITY_COST : t.constraint.priorityCost();
    }

    public TransferPoint getFrom() {
        return from;
    }

    public TransferPoint getTo() {
        return to;
    }

    public TransferConstraint getConstraint() {
        return constraint;
    }

    public boolean includeSlack() {
        return !constraint.isFacilitated();
    }

    public boolean noConstraints() {
        return constraint.noConstraints();
    }

    public boolean matchesStopPos(int fromStopPos, int toStopPos) {
        return from.getStopPosition() == fromStopPos && to.getStopPosition() == toStopPos;
    }

    /**
     * Maximum time after scheduled departure time the connecting transport is guarantied to wait
     * for the delayed trip.
     */
    public int getMaxWaitTime() {
        return getConstraint().getMaxWaitTime();
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
        return Objects.hash(from, to, constraint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof ConstrainedTransfer)) { return false; }
        final ConstrainedTransfer transfer = (ConstrainedTransfer) o;
        return Objects.equals(constraint, transfer.constraint)
                && Objects.equals(from, transfer.from)
                && Objects.equals(to, transfer.to);
    }

    public String toString() {
        return ToStringBuilder.of(ConstrainedTransfer.class)
                .addObj("from", from)
                .addObj("to", to)
                .addObj("constraint", constraint)
                .toString();
    }
}
