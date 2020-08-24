package org.opentripplanner.graph_builder.module.time;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;

import java.io.File;
import java.util.HashMap;


public class TraficPredictionBuildeModule implements GraphBuilderModule {
private File traficprediction;
private  clusterlist clusterlist;

    public TraficPredictionBuildeModule(File traficprediction) {
        this.traficprediction = traficprediction;
        this.clusterlist = new clusterlist(traficprediction);
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
    graph.setClusters(this.clusterlist);
    for (cluster c : graph.getClusters().getclusters()) {


    }
    }

    @Override
    public void checkInputs() {

    }
}
