package org.opentripplanner.routing.edgetype.loader;

import java.util.List;

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.Transfer;
import org.opentripplanner.routing.vertextypes.Intersection;
import org.opentripplanner.routing.vertextypes.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

public class NetworkLinker {
    private Graph graph;

    public NetworkLinker(Graph graph) {
        this.graph = graph;
    }

    @SuppressWarnings("unchecked")
    public void createLinkage() {

        STRtree index = new STRtree();
        for (Vertex v : graph.getVertices()) {
            index.insert(new Envelope(v.getCoordinate()), v);
        }

        for (Vertex v : graph.getVertices()) {
            if (v.getType() == TransitStop.class) {
                // find nearby vertices

                Envelope env = new Envelope(v.getCoordinate());
                env.expandBy(0.0018); 
                List<Vertex> nearby = (List<Vertex>) index.query(env);

                Vertex nearestIntersection = null;
                double minDistance = 1000000000;
                Coordinate coord = v.getCoordinate();
                double lat1 = coord.y;
                double lon1 = coord.x;
                for (Vertex nv : nearby) {
                    if (nv == v) {
                        continue;
                    }
                    if (nv.distance(v) > 200) {
                        //STRtree.query does not guarantee that all found points will actually be within the envelope
                        continue;
                    }
                    coord = nv.getCoordinate();
                    double lat2 = coord.y;
                    double lon2 = coord.x;
                    double distance = GtfsLibrary.distance(lat1, lon1, lat2, lon2) * 2;

                    if (nv.getType() == Intersection.class) {
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestIntersection = nv;
                        }
                    }
                }

                if (nearestIntersection != null) {
                    graph.addEdge(new StreetTransitLink(nearestIntersection, v, true));
                    graph.addEdge(new StreetTransitLink(v, nearestIntersection, false));
                }
            }
        }
    }
}
