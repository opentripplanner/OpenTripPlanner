package org.opentripplanner.model.plan.walkstep.verticaltransportation;

import javax.annotation.Nullable;
import org.opentripplanner.service.streetdetails.model.Level;

/**
 * Represents information about a single use of an escalator related to
 * {@link org.opentripplanner.model.plan.walkstep.WalkStep}.
 */
public final class EscalatorUse extends VerticalTransportationUse {

  public EscalatorUse(
    @Nullable Level from,
    @Nullable Level to,
    VerticalDirection verticalDirection
  ) {
    super(from, to, verticalDirection);
  }
}
