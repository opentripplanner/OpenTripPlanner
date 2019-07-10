package org.opentripplanner.graph_builder.module;

import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopStreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

/**
 * Default way to connect vertexes.
 */
public class DefaultVertexConnector implements VertexConnector {
	
	private static final Logger LOG = LoggerFactory.getLogger(DefaultVertexConnector.class);
	
	@Override
	public boolean connectVertex(TransitStop stop, boolean wheelchairAccessible, Collection<Vertex> vertices) {
		// Search for a matching stop code in (nearby) vertices that represent transit stops in OSM.
		Optional<TransitStopStreetVertex> optionalVertex = findFirstVertexWithMatchingStopCode(stop, vertices);
		
		// Create a link if a stop code in a vertex's ref= tag matches the GTFS stop code of this TransitStop.
		return optionalVertex.isPresent() && makeStreetTransitLink(stop, wheelchairAccessible, optionalVertex.get());
	}
	
	private Optional<TransitStopStreetVertex> findFirstVertexWithMatchingStopCode(TransitStop stop, Collection<Vertex> vertices) {
		return vertices.stream()
					.filter(TransitStopStreetVertex.class::isInstance)
					.map(TransitStopStreetVertex.class::cast)
					.filter(vertex -> matchStopCodes(vertex.stopCode, stop.getStopCode()))
					.findFirst();
	}
	
	private boolean matchStopCodes(String vertexStopCode, String stopCode) {
		if (vertexStopCode == null)
			return false;
		
		return stopCode.equals(vertexStopCode);
	}
	
	private boolean makeStreetTransitLink(TransitStop ts, boolean wheelchairAccessible, TransitStopStreetVertex tsv) {
		new StreetTransitLink(ts, tsv, wheelchairAccessible);
		new StreetTransitLink(tsv, ts, wheelchairAccessible);
		LOG.debug("Connected {} ({}) to {} at {}", ts.toString(), ts.getStopCode(), tsv.getLabel(), tsv.getCoordinate().toString());
		return true;
	}
	
}
