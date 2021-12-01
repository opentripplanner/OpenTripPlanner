package org.opentripplanner.ext.dataOverlay;


import org.opentripplanner.ext.dataOverlay.configuration.DavaOverlayConfig;
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
    private final DavaOverlayConfig fileConfiguration;

    /**
     * Sets the generic grid data file
     *
     * @param dataFile generic data file
     * @param configuration corresponding configuration
     */
    public EdgeUpdaterModule(GenericDataFile dataFile, DavaOverlayConfig configuration) {
        this.dataFile = dataFile;
        this.fileConfiguration = configuration;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra, DataImportIssueStore issueStore) {
        GenericEdgeUpdater genericEdgeUpdater = new GenericEdgeUpdater(dataFile, fileConfiguration, graph.getStreetEdges());
        genericEdgeUpdater.updateEdges();
    }

    @Override
    public void checkInputs() {
        //nothing
    }
}
