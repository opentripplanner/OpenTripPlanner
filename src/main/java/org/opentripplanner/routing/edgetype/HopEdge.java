package org.opentripplanner.routing.edgetype;

import org.opentripplanner.model.Stop;

import org.locationtech.jts.geom.LineString;

/**
 * FrequencyHops and PatternHops have start/stop Stops
 * @author novalis
 *
 */
public interface HopEdge {

    Stop getEndStop();

    Stop getBeginStop();

    void setGeometry(LineString geometry);

    String getFeedId();
}
