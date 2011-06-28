package org.opentripplanner.routing.edgetype;

import org.onebusaway.gtfs.model.Stop;

/**
 * Hops and PatternHops have start/stop Stops
 * @author novalis
 *
 */
public interface HopEdge {

	Stop getEndStop();

	Stop getStartStop();

}
