package org.opentripplanner.jags.edgetype.loader;

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.SpatialVertex;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.StreetTransitLink;
import org.opentripplanner.jags.edgetype.Transfer;
import org.opentripplanner.jags.gtfs.GtfsLibrary;
import org.opentripplanner.jags.vertextypes.Intersection;
import org.opentripplanner.jags.vertextypes.TransitStop;


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
			if (v.type == TransitStop.class && v instanceof SpatialVertex) {
				//find nearby vertices
				SpatialVertex sv = (SpatialVertex) v;

				Envelope env = new Envelope(sv.getCoordinate());
				env.expandBy(0.0018); //FIXME: meters?
				List<SpatialVertex> nearby = (List<SpatialVertex>) index.query(env);
				
				SpatialVertex nearestIntersection = null;
				double minDistance = 1000000000;
				Coordinate coord = sv.getCoordinate();
				double lat1 = coord.y;
				double lon1 = coord.x;
				for (SpatialVertex nv : nearby) {
					if (nv == sv) {
						continue;
					}
					coord = nv.getCoordinate();
					double lat2 = coord.y;
					double lon2 = coord.x;
					double distance = GtfsLibrary.distance(lat1, lon1, lat2, lon2) * 2;
					
					if (nv.type == TransitStop.class) {
						graph.addEdge(sv, nv, new Transfer(distance));
						graph.addEdge(nv, sv, new Transfer(distance));
					} else if (nv.type == Intersection.class) {
						if (distance < minDistance) {
							minDistance = distance;
							nearestIntersection = nv;
						}
					}
				}
				
				if (nearestIntersection != null) {
					graph.addEdge(nearestIntersection, sv, new StreetTransitLink());
					graph.addEdge(sv, nearestIntersection, new StreetTransitLink());
				}
			}
		}
	}
}
