package org.opentripplanner.model.plan.legreference;

import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.service.TransitService;

/**
 * Marker interface for various types of leg references
 */
public interface LegReference {
  Leg getLeg(TransitService transitService);
}
