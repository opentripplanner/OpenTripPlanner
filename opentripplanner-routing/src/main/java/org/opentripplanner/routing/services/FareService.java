package org.opentripplanner.routing.services;

import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.spt.GraphPath;

/**
 * Computes a fare for a given GraphPath.
 * @author novalis
 *
 */
public interface FareService {
	public Fare getCost(GraphPath path);
}
