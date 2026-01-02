package org.opentripplanner.model.plan.walkstep.verticaltransportation;

import javax.annotation.Nullable;
import org.opentripplanner.service.streetdetails.model.Level;

/**
 * Represents information about a single use of an elevator related to
 * {@link org.opentripplanner.model.plan.walkstep.WalkStep}.
 */
public final class ElevatorUse extends VerticalTransportationUse {

  public ElevatorUse(
    @Nullable Level from,
    @Nullable Level to,
    VerticalDirection verticalDirection
  ) {
    super(from, to, verticalDirection);
  }
}
