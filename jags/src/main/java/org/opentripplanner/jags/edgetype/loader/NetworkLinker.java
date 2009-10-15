package org.opentripplanner.jags.edgetype.loader;

import java.util.List;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.SpatialVertex;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.Alight;
import org.opentripplanner.jags.edgetype.Board;
import org.opentripplanner.jags.edgetype.Transfer;


public class NetworkLinker {
	private Graph graph;
	
	public NetworkLinker(Graph graph) {
		this.graph = graph;
	}
	
	public void createLinkage() {
		
		STRtree index = new STRtree(); 
		for (Vertex v : graph.getVertices()) {
			if (v instanceof SpatialVertex) {
				SpatialVertex sv = (SpatialVertex) v;
				index.insert(new Envelope(sv.getCoordinate()), v);
			}
		}
		
		for(Vertex v : graph.getVertices()) { 
			if (v.isTransitStop && v instanceof SpatialVertex) {
				//find nearby vertices
				SpatialVertex sv = (SpatialVertex) v;

				Envelope env = new Envelope(sv.getCoordinate());
				env.expandBy(0.0018); //FIXME: meters?
				List<SpatialVertex> nearby = (List<SpatialVertex>) index.query(env);
				
				SpatialVertex nearestNonTransitVertex = null;
				double minDistance = 0;
				for (SpatialVertex nv : nearby) {
					if (nv == sv) {
						continue;
					}
					double distance = nv.getCoordinate().distance(sv.getCoordinate());
					if (nv.isTransitStop) {
						graph.addEdge(sv, nv, new Transfer(distance));
						graph.addEdge(nv, sv, new Transfer(distance));
					} else {
						if (distance < minDistance) {
							minDistance = distance;
							nearestNonTransitVertex = nv;
						}
					}
				}
				
				if (nearestNonTransitVertex != null) {
					graph.addEdge(nearestNonTransitVertex, sv, new Board());
					graph.addEdge(sv, nearestNonTransitVertex, new Alight());
				}
			}
		}
	}
}
