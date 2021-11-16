package org.opentripplanner.ext.airquality;


import org.opentripplanner.ext.airquality.configuration.GenericFileConfiguration;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;

import java.util.HashMap;

/**
 * This class allows updating the graph with the grid data from generic .nc file in accordance with provided
 * json configuration
 *
 * @author Simeon Platonov
 */
public class EdgeUpdaterModule implements GraphBuilderModule {
    private final GenericDataFile dataFile;
    private final GenericFileConfiguration fileConfiguration;

    /**
     * Sets the generic grid data file
     *
     * @param dataFile generic data file
     * @param configuration corresponding configutation
     */
    public EdgeUpdaterModule(GenericDataFile dataFile, GenericFileConfiguration configuration) {
        this.dataFile = dataFile;
        this.fileConfiguration = configuration;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra, DataImportIssueStore issueStore) {
        GenericEdgeUpdater genericEdgeUpdater = new GenericEdgeUpdater(dataFile, fileConfiguration, graph.getStreetEdges());
        genericEdgeUpdater.updateEdges();
    }

    @Override
    public void checkInputs() throws Exception {
        if (!dataFile.isValid()) {
            throw new Exception(dataFile.getError());
        }
    }
}
