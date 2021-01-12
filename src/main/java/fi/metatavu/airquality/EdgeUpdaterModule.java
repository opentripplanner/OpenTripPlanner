package fi.metatavu.airquality;


import fi.metatavu.airquality.configuration_parsing.SingleConfig;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;

import java.util.HashMap;

/**
 * A module to update graph with air quality data
 */
public class EdgeUpdaterModule implements GraphBuilderModule {
    private final GenericDataFile dataFile;
    private final SingleConfig config;

    public EdgeUpdaterModule(GenericDataFile dataFile, SingleConfig config) {
        this.dataFile = dataFile;
        this.config = config;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra, DataImportIssueStore issueStore) {
        GenericEdgeUpdater airQualityEdgeUpdater = new GenericEdgeUpdater(dataFile, graph.getStreetEdges(), config);
        airQualityEdgeUpdater.updateEdges();
    }

    @Override
    public void checkInputs() throws Exception {
        if (!dataFile.isValid()) {
            throw new Exception(dataFile.getError());
        }
    }
}
