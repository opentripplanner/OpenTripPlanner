package org.opentripplanner.model.transfer;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A constrained transfer is a transfer which is restricted in one ore more ways by the transit data
 * provider. It can be guaranteed or stay-seated, have a priority (NOT_ALLOWED, ALLOWED,
 * RECOMMENDED, PREFERRED) or some sort of time constraint attached to it. It is applied to a
 * transfer from a transfer-point to another point. A transfer point is a combination of stop and
 * route/trip.
 */
public final class ConstrainedTransfer implements RaptorConstrainedTransfer, Serializable {

  private static final int FROM_RANKING_COEFFICIENT = 11;
  private static final int TO_RANKING_COEFFICIENT = 10;

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
   * In NeTEx an interchange have an id, in GTFS a transfer do not. We include it here to enable
   * debugging, logging and system integration. Note! OTP do not use this id, and it is just passed
   * through OTP. There is no service in OTP to look up a transfer by its id.
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
    return constraint.isRegularTransfer();
  }

  /**
   * <a href="https://developers.google.com/transit/gtfs/reference/gtfs-extensions#specificity-of-a-transfer">
   * Specificity of a transfer
   * </a>
   * <p>
   * The ranking implemented here is slightly modified:
   * <ul>
   *     <li>
   *         The specification do not say anything about Stations even if Stations can be used to
   *         specify a transfer-point. In OTP stops are more specific than station, so we use the
   *         following transfer-point ranking:
   *         <ol>
   *             <li>Station: 0 (zero)</li>
   *             <li>Stop: 1</li>
   *             <li>Route: 2</li>
   *             <li>Trip: 3</li>
   *         </ol>
   *     </li>
   *     <li>
   *         Two transfers may have the same ranking if we add together the from-point and
   *         to-point ranking.
   *         For example, {@code from trip(3) + to stop(1) == from route(2) + to route(2)}
   *         have the same ranking. To avoid this problem, we give the from-point a small
   *         advantage. We multiply the from point with 11 and the to point with 10, this
   *         break the ties in favor of the from point. In the example above the
   *         ConstrainedTransfer specificityRanking is:
   * <pre>
   * Case 1: from trip to stop :=  11 * 3 + 10 * 1 = 43
   * Case 2: from route to route :=  11 * 2 + 10 * 2 = 42
   * </pre>
   *         Case 1 has the highest ranking.
   *     </li>
   * </ul>
   */
  public int getSpecificityRanking() {
    return (
      from.getSpecificityRanking() * FROM_RANKING_COEFFICIENT +
      to.getSpecificityRanking() * TO_RANKING_COEFFICIENT
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, constraint);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConstrainedTransfer)) {
      return false;
    }
    final ConstrainedTransfer transfer = (ConstrainedTransfer) o;
    return (
      Objects.equals(constraint, transfer.constraint) &&
      Objects.equals(from, transfer.from) &&
      Objects.equals(to, transfer.to)
    );
  }

  public String toString() {
    return ToStringBuilder.of(ConstrainedTransfer.class)
      .addObj("from", from)
      .addObj("to", to)
      .addObj("constraint", constraint)
      .toString();
  }
}
