package org.opentripplanner.model.plan.walkstep.verticaltransportation;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.service.streetdetails.model.Level;

/**
 * Represents information about a single use of vertical transportation equipment stored in
 * {@link org.opentripplanner.model.plan.walkstep.WalkStep}.
 */
public abstract sealed class VerticalTransportationUse
  permits ElevatorUse, EscalatorUse, StairsUse {

  private final Level from;
  private final Level to;
  private final VerticalDirection verticalDirection;

  public VerticalTransportationUse(
    @Nullable Level from,
    @Nullable Level to,
    VerticalDirection verticalDirection
  ) {
    this.from = from;
    this.to = to;
    this.verticalDirection = Objects.requireNonNull(verticalDirection);
  }

  /**
   * Can be null when no valid level info was found. This is the case, for example, when level
   * information for an elevator is not set in OSM and OTP uses the default level, or when level
   * info is not in the correct format for inclined edges.
   */
  @Nullable
  public Level from() {
    return this.from;
  }

  /**
   * Can be null when no valid level info was found. This is the case, for example, when level
   * information for an elevator is not set in OSM and OTP uses the default level, or when level
   * info is not in the correct format for inclined edges.
   */
  @Nullable
  public Level to() {
    return this.to;
  }

  public VerticalDirection verticalDirection() {
    return this.verticalDirection;
  }
}
