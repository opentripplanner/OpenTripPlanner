package org.opentripplanner.api.thrift.util;

import org.opentripplanner.api.thrift.definition.GraphEdge;
import org.opentripplanner.routing.graph.Edge;


/**
 * Extends the Thrift GraphVertex
 * 
 * @author avi
 * 
 */
public class GraphEdgeExtension extends GraphEdge {

	/**
	 * Required for serialization.
	 */
	private static final long serialVersionUID = 4282396235874134842L;

	/**
	 * Construct from an Edge object.
	 * 
	 * @param e
	 */
	public GraphEdgeExtension(Edge e) {
		super();

		setId(e.getId());
		setHead(new GraphVertexExtension(e.getFromVertex()));
		setTail(new GraphVertexExtension(e.getToVertex()));
		setName(e.getName());
	}

}