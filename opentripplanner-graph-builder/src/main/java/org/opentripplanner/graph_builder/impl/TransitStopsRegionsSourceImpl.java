package org.opentripplanner.graph_builder.impl;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.graph_builder.services.RegionsSource;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.vertextypes.TransitStop;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Envelope;

public class TransitStopsRegionsSourceImpl implements RegionsSource {

    private Graph _graph;

    @Autowired
    public void setGraph(Graph graph) {
        _graph = graph;
    }

    @Override
    public Iterable<Envelope> getRegions() {

        List<Envelope> regions = new ArrayList<Envelope>();

        for (Vertex vertex : _graph.getVertices()) {
            if (vertex.getType()  == TransitStop.class) { 
                Envelope env = new Envelope(vertex.getCoordinate());
                // TODO - Would be nice to express this in meters
                env.expandBy(0.02);
                regions.add(env);
            }
        }

        return regions;
    }
}
