package org.opentripplanner.graph_builder.module.vehicle_sharing;

import javafx.scene.input.TransferMode;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.rentedgetype.RentCarAnywhereEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import java.util.HashMap;

public class CarSharingBuilderModule implements GraphBuilderModule {
    TransferMode MODE;
    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        for(Vertex vertex: graph.getVertices()){
            new RentCarAnywhereEdge(vertex,vertex,5,5);
        }
    }

    @Override
    public void checkInputs() {

    }
}
