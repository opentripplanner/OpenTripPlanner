package org.opentripplanner.analyst.batch;

import javax.annotation.PostConstruct;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class GraphGeographicFilter implements IndividualFilter {

    @Autowired private RoutingRequest prototypeRoutingRequest;
    @Autowired private GraphService graphService;

    private Geometry hull;
    
    @PostConstruct
    public void findHull() {
        Graph graph= graphService.getGraph(prototypeRoutingRequest.getRouterId());
        //find hull of all stops, then buffer the resulting geom
    }
    
    @Override
    public boolean filter(Individual individual) {
        Point p = new Point(individual.lon, individual.lat);
        return hull.contains(p);
    }

}
