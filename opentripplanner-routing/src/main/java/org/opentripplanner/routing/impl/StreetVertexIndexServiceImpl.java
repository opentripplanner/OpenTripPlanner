package org.opentripplanner.routing.impl;

import java.util.List;

import javax.annotation.PostConstruct;

import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextypes.Intersection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

@Component
public class StreetVertexIndexServiceImpl implements StreetVertexIndexService {

    private Graph _graph;

    private STRtree _index;

    @Autowired
    public void setGraph(Graph graph) {
        _graph = graph;
    }

    @PostConstruct
    public void setup() {

        _index = new STRtree();

        for (Vertex v : _graph.getVertices()) {
            if (v.getType() == Intersection.class)
                _index.insert(new Envelope(v.getCoordinate()), v);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Vertex getClosestVertex(Coordinate location) {
        
        Envelope env = new Envelope(location);
        env.expandBy(0.0018); 
        List<Vertex> nearby = (List<Vertex>) _index.query(env);

        Vertex minVertex = null;
        double minDistance = Double.POSITIVE_INFINITY;
        
        double lat1 = location.y;
        double lon1 = location.x;
        
        for (Vertex nv : nearby) {
            Coordinate coord = nv.getCoordinate();
            double lat2 = coord.y;
            double lon2 = coord.x;
            double distance = DistanceLibrary.distance(lat1, lon1, lat2, lon2);
            if( distance < minDistance ) {
                minVertex = nv;
                minDistance = distance;
            }
        }
        
        return minVertex;
    }

}
