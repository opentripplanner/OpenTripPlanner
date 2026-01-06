package org.opentripplanner.model.plan.walkstep.verticaltransportation;

import javax.annotation.Nullable;
import org.opentripplanner.service.streetdetails.model.Level;

/**
 * Represents information about a single use of a set of stairs related to
 * {@link org.opentripplanner.model.plan.walkstep.WalkStep}.
 */
public final class StairsUse extends VerticalTransportationUse {

  public StairsUse(@Nullable Level from, @Nullable Level to, VerticalDirection verticalDirection) {
    super(from, to, verticalDirection);
  }
}
