/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.transfer;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTransfer;


/**
 * A constrained transfer is a transfer which is restricted in one or more ways by the transit data
 * provider. It can be guaranteed or stay-seated, have a priority (NOT_ALLOWED, ALLOWED,
 * RECOMMENDED, PREFERRED) or some sort of time constraint attached to it. It is applied to a
 * transfer from a transfer-point to another point. A transfer point is a combination of stop and
 * route/trip.
 */
public final class ConstrainedTransfer implements RaptorConstrainedTransfer, Serializable {

    private static final long serialVersionUID = 1L;

    private final FeedScopedId id;

    private final TransferPoint from;

    private final TransferPoint to;

    private final TransferConstraint constraint;

    public ConstrainedTransfer(
            @Nullable FeedScopedId id,
            TransferPoint from,
            TransferPoint to,
            TransferConstraint constraint
    ) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.constraint = constraint;
    }

    /**
     * In NeTEx an interchange have an id, in GTFS a transfer do not. We include it here to
     * enable debugging, logging and system integration. Note! OTP do not use this id,
     * and it is just passed through OTP. There is no service in OTP to look up a transfer by its
     * id.
     */
    @Nullable
    public FeedScopedId getId() {
        return id;
    }

    public TransferPoint getFrom() {
        return from;
    }

    public TransferPoint getTo() {
        return to;
    }

    @Override
    public TransferConstraint getTransferConstraint() {
        return constraint;
    }

    public boolean noConstraints() {
        return constraint.noConstraints();
    }

    public boolean matchesStopPos(int fromStopPos, int toStopPos) {
        return from.getStopPosition() == fromStopPos && to.getStopPosition() == toStopPos;
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
