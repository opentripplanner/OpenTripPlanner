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
 * Vertex connector logic for HSL. HSL has cases where Stop code includes H prefix, which may cause issues in some stops.
 */
public class HSLVertexConnector implements VertexConnector {
	
	private static final Logger LOG = LoggerFactory.getLogger(HSLVertexConnector.class);
	
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
		
		if (stopCode.equals(vertexStopCode))
			return true;
		
		if (Math.abs(vertexStopCode.length() - stopCode.length()) != 1)
			return false;
		
		return matchPrefixedHslStopCodes(vertexStopCode, stopCode)
			|| matchPrefixedHslStopCodes(stopCode, vertexStopCode);
	}
	
	private static boolean matchPrefixedHslStopCodes(String potentiallyPrefixedStopCode, String stopCode) {
		// Special check for HSL stops which may have a prefixed 'H'
		boolean hasHslPrefix = ('H' == potentiallyPrefixedStopCode.charAt(0));
		return hasHslPrefix && stopCode.equals(potentiallyPrefixedStopCode.substring(1));
	}
	
	private boolean makeStreetTransitLink(TransitStop ts, boolean wheelchairAccessible, TransitStopStreetVertex tsv) {
		new StreetTransitLink(ts, tsv, wheelchairAccessible);
		new StreetTransitLink(tsv, ts, wheelchairAccessible);
		LOG.debug("Connected {} ({}) to {} at {}", ts.toString(), ts.getStopCode(), tsv.getLabel(), tsv.getCoordinate().toString());
		return true;
	}
	
}
