package org.opentripplanner.graph_builder.module;

import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopStreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Default way to connect vertexes.
 */
public class DefaultVertexConnector implements VertexConnector {


	private static final Logger LOG = LoggerFactory.getLogger(TransitToTaggedStopsModule.class);

	private void makeStreetTransitLink(TransitStop ts, boolean wheelchairAccessible, TransitStopStreetVertex tsv) {
		new StreetTransitLink(ts, tsv, wheelchairAccessible);
		new StreetTransitLink(tsv, ts, wheelchairAccessible);
		LOG.debug("Connected " + ts.toString() + " (" + ts.getStopCode() + ") to " + tsv.getLabel() + " at " + tsv.getCoordinate().toString());
	}

	@Override
	public boolean connectVertex(TransitStop ts, boolean wheelchairAccessible,  Collection<Vertex> vertices) {
		// Iterate over all nearby vertices representing transit stops in OSM, linking to them if they have a stop code
		// in their ref= tag that matches the GTFS stop code of this TransitStop.
		for (Vertex v : vertices) {
			if (!(v instanceof TransitStopStreetVertex)) {
				continue;
			}

			TransitStopStreetVertex tsv = (TransitStopStreetVertex) v;
			// Only use stop codes for linking TODO: find better method to connect stops without stop code
			if (tsv.stopCode != null && tsv.stopCode.equals(ts.getStopCode())) {
				makeStreetTransitLink(ts, wheelchairAccessible, tsv);
				return true;
			}
		}
		return false;
	}
}
