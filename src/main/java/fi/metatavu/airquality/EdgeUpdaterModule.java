package fi.metatavu.airquality;


import fi.metatavu.airquality.configuration_parsing.GenericFileConfiguration;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;

import java.util.HashMap;

/**
 * A module to update graph with generic data according to one of the configurations in the list
 */
public class EdgeUpdaterModule implements GraphBuilderModule {
    private final GenericDataFile dataFile;


    public EdgeUpdaterModule(GenericDataFile dataFile) {
        this.dataFile = dataFile;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra, DataImportIssueStore issueStore) {
        GenericEdgeUpdater genericEdgeUpdater = new GenericEdgeUpdater(dataFile, graph.getStreetEdges());
        genericEdgeUpdater.updateEdges();
    }

    @Override
    public void checkInputs() throws Exception {
        if (!dataFile.isValid()) {
            throw new Exception(dataFile.getError());
        }
    }
}
