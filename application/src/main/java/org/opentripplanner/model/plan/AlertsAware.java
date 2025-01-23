package org.opentripplanner.model.plan;

import java.util.Set;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public interface AlertsAware<T> {
  Set<TransitAlert> getTransitAlerts();
  /**
   * Returns a copy of the entity with alerts added to it.
   */
  T decorateWithAlerts(Set<TransitAlert> alerts);
}
