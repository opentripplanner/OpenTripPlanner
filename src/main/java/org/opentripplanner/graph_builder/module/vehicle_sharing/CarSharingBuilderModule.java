package org.opentripplanner.graph_builder.module.vehicle_sharing;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.RentCarAnywhereEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import java.util.HashMap;

public class CarSharingBuilderModule implements GraphBuilderModule {

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        for(Vertex vertex: graph.getVertices()){
            if(vertex.getIncoming().stream().anyMatch(e->e instanceof StreetEdge))
                new RentCarAnywhereEdge(vertex, 1, 1);
        }
    }

    @Override
    public void checkInputs() {

    }
}
