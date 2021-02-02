package fi.metatavu.airquality;


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

    /**
     * Sets the generic grid data file
     *
     * @param dataFile generic data file
     */
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
