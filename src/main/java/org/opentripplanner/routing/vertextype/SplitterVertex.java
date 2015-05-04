package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;

public class SplitterVertex extends IntersectionVertex {
	public SplitterVertex(Graph g, String label, double x, double y) {
		super(g, label, x, y);
		// splitter vertices don't represent something that exists in the world, so traversing them is
		// always free.
		this.freeFlowing = true;
	}
	
	/** SplitterVertices have opposites that are the split on the back edge */
	public SplitterVertex opposite;

	private static final long serialVersionUID = 1L;

}
