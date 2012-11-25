package org.opentripplanner.api.thrift.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opentripplanner.api.thrift.definition.GraphVertex;
import org.opentripplanner.api.thrift.definition.Location;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Coordinate;

public class GraphUtil {

	/**
	 * Makes a Thrift GraphVertex structure from a Vertex.
	 * 
	 * @param v
	 * @return
	 */
	public static GraphVertex makeGraphVertex(Vertex v) {
		GraphVertex gv = new GraphVertex();
		gv.setLabel(v.getLabel());
		Coordinate coord = v.getCoordinate();
		if (coord != null) {
			gv.setLocation(LocationUtil.makeLocation(coord));
		}

		gv.setName(v.getName());
		gv.setIn_degree(v.getDegreeIn());
		gv.setOut_degree(v.getDegreeOut());
		return gv;
	}

	/**
	 * Returns all vertices in the graph as GraphVertices.
	 * @param g
	 * @return
	 */
	public static List<GraphVertex> getGraphVertices(Graph g) {
		Collection<Vertex> verts = g.getVertices();
		List<GraphVertex> l = new ArrayList<GraphVertex>(verts.size());
		for (Vertex v : verts) {
			l.add(makeGraphVertex(v));
		}
		return l;
	}
}
