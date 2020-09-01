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
        TreeMap<Integer, Integer> map = new TreeMap<>();
        for (Cluster c : this.clusterlist.getclusters()) {
            for (EdgeData e : c.getedges())
                if (c.gettimetable() != null)
                    map.put(((int) e.getid()), e.getclusterid() - 1);
        }

        for (StreetEdge e : graph.getStreetEdges()) {
            if (map.get(e.getId()) != null) {
                int clid = map.get(e.getId());
                e.setTimes((this.clusterlist.getclusters().get(clid)).gettimetableas());
                e.getTimes().sort(TimeTable::compareTo);

            }
        }
    }
    @Override
    public void checkInputs() {

    }
}
