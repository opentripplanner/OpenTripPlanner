package org.opentripplanner.graph_builder.module.time;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;


public class TraficPredictionBuildeModule implements GraphBuilderModule {
private final clusterlist clusterlist;

    public TraficPredictionBuildeModule(File traficprediction) {
        this.clusterlist = new clusterlist(traficprediction);
        this.clusterlist.getclusters().sort(Comparator.naturalOrder());
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
    graph.setClusters(this.clusterlist);
        TreeMap<Integer,Integer> map = new TreeMap<>();
    for (cluster c : graph.getClusters().getclusters()) {
        for( edgedata e: c.getedges())
            map.put( ((int) e.getid()),e.getclusterid());
    }
    for(StreetEdge e : graph.getStreetEdges())
    {
        e.setTimes( this.clusterlist.getclusters().get(map.get(e.getId())).gettimetableas());
    }
    }

    @Override
    public void checkInputs() {

    }
}
