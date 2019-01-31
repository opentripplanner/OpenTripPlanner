package org.opentripplanner.routing.transit_index;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.GeometrySerializer;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.graph.Edge;

import java.io.Serializable;

/**
 * RouteSegment holds the edges around one stop on particular trip or pattern.
 * We can use this if we patch the graph to remove the stop from the
 * trip/pattern.
 */
public class RouteSegment implements Serializable  {
	private static final long serialVersionUID = -3486047425509893460L;
	public Edge hopIn;
	public Edge hopOut;
	public Edge board;
	public Edge alight;
	public Edge dwell;
	public FeedScopedId stop;
	public RouteSegment(FeedScopedId stop) {
		this.stop = stop;
	}

	@JsonSerialize(using= GeometrySerializer.class)
	public Geometry getGeometry() {
	    return hopOut.getGeometry();
	}
}
