package org.opentripplanner.graph_builder.module;

import com.google.common.collect.Iterables;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.stopsAlerts.IStopTester;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * It prints the stops that satisfy certain criteria.
 * The output is a list of stops, some of the stops attributes (coordinates and etc.) and the criteria it satisfies.
 */

public class StopsAlerts implements GraphBuilderModule {

    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(StopsAlerts.class);

    List<IStopTester> stopTesters = new ArrayList<IStopTester>();

    String logFile = "";

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {
        try {
            PrintWriter pw = new PrintWriter(new File(logFile));
            pw.printf("%s,%s,%s,%s\n","stopId","lon","lat","types");
            for (TransitStopVertex ts : graph.getVerticesOfType(TransitStopVertex.class)) {
                StringBuilder types = new StringBuilder();
                for(IStopTester stopTester:stopTesters){
                    if(stopTester.fulfillDemands(ts,graph)){
                        if(types.length() > 0) types.append(";");
                        types.append(stopTester.getType());
                    }
                }
                if(types.length() > 0) {
                    pw.printf("%s,%f,%f,%s\n",ts.getStop().getId(), ts.getCoordinate().x,
                            ts.getCoordinate().y, types.toString());
                }
            }
            pw.close();            
        } catch (FileNotFoundException e) {
            LOG.error("Failed to write StopsAlerts log file", e);
        }
    }

    @Override
    public void checkInputs() {
        if(logFile.isEmpty())
            throw new RuntimeException("missing log file name");
    }
}
