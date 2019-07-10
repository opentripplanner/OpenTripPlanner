package org.opentripplanner.graph_builder.module;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.Collection;

public interface VertexConnector {
	
	boolean connectVertex(TransitStop ts, boolean wheelchairAccessible, Collection<Vertex> vertices);
	
}
