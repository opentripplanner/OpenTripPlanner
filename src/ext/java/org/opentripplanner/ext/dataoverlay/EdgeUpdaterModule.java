package org.opentripplanner.ext.dataoverlay;


import java.util.HashMap;
import org.opentripplanner.ext.dataoverlay.configuration.TimeUnit;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;

/**
 * This class allows updating the graph with the grid data from generic .nc file in accordance with
 * provided json configuration
 *
 * @author Simeon Platonov
 */
public class EdgeUpdaterModule implements GraphBuilderModule {

    private final GenericDataFile dataFile;
    private final TimeUnit timeFormat;

    /**
     * Sets the generic grid data file
     */
    public EdgeUpdaterModule(GenericDataFile dataFile, TimeUnit timeFormat) {
        this.dataFile = dataFile;
        this.timeFormat = timeFormat;
    }

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {
        GenericEdgeUpdater genericEdgeUpdater = new GenericEdgeUpdater(
                dataFile, timeFormat, graph.getStreetEdges()
        );
        genericEdgeUpdater.updateEdges();
    }

    @Override
    public void checkInputs() {
        //nothing
    }
}
