package org.opentripplanner.graph_builder.module.time;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;


public class TrafifcPredictionBuilderModule implements GraphBuilderModule {
    private final ClusterList clusterlist;

    public TrafifcPredictionBuilderModule(File traficprediction) {
        this.clusterlist = new ClusterList(traficprediction);
        this.clusterlist.getclusters().sort(Comparator.naturalOrder());
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        graph.setClusters(this.clusterlist);
        TreeMap<Integer, Integer> map = new TreeMap<>();
        for (Cluster c : graph.getClusters().getclusters()) {
            for (EdgeData e : c.getedges())
                map.put(((int) e.getid()), e.getclusterid());
        }
        for (StreetEdge e : graph.getStreetEdges()) {
            if (this.clusterlist.getclusters()!=null
                    && this.clusterlist.getclusters().get(map.get(e.getId()) )!=null
                    && this.clusterlist.getclusters().get(map.get(e.getId()) ).gettimetable()!=null
                ){
            e.setTimes(this.clusterlist.getclusters().get(map.get(e.getId())).gettimetableas());
            e.getTimes().sort(TimeTable::compareTo);
        }}
    }

    @Override
    public void checkInputs() {

    }
}
