package org.opentripplanner.graph_builder.module;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.common.StreetUtils;
import org.opentripplanner.graph_builder.linking.TransitToStreetNetworkModule;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.*;

/**
 * this module is part of the  {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} process. it design to remove small isolated
 * islands form the graph. Islands are created when there is no connectivity in the map, island 
 * acts like trap since there is no connectivity there is no way in or out the island.
 * The module distinguish between two island types one with transit stops and one without stops.
 */
public class PruneFloatingIslands implements GraphBuilderModule {

    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(PruneFloatingIslands.class);

    /**
     * this field indicate the maximum size for island without stops
     * island under this size will be pruned.
     */
    private int pruningThresholdIslandWithoutStops;

    /**
     * this field indicate the maximum size for island with stops
     * island under this size will be pruned.
     */
    private int pruningThresholdIslandWithStops;

    /**
     * The name for output file for this process. The file will store information about the islands 
     * that were found and whether they were pruned. If the value is an empty string or null there 
     * will be no output file.
     */
    private String islandLogFile;

    private StreetLinkerModule transitToStreetNetwork;

    public List<String> provides() {
        return Collections.emptyList();
    }

    public List<String> getPrerequisites() {
        /**this module can run after the street module only but if
         * the street linker did not run then it couldn't identifies island with stops.
         * so if the need is to distinguish between island with stops or without stops
         * as explained before this module should run after the streets and the linker modules.
         */
        return Arrays.asList("streets");
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Pruning isolated islands in street network");
        
        StreetUtils.pruneFloatingIslands(graph, pruningThresholdIslandWithoutStops, 
        		pruningThresholdIslandWithStops, islandLogFile);
        if (transitToStreetNetwork == null) {
            LOG.debug("TransitToStreetNetworkGraphBuilder was not provided to PruneFloatingIslands. Not attempting to reconnect stops.");
        } else {
            //reconnect stops on small islands (that removed)
            transitToStreetNetwork.buildGraph(graph,extra);
        }
        LOG.debug("Done pruning isolated islands");
    }

    @Override
    public void checkInputs() {
        //no inputs
    }
    public void setPruningThresholdIslandWithoutStops(int pruningThresholdIslandWithoutStops) {
    	this.pruningThresholdIslandWithoutStops = pruningThresholdIslandWithoutStops;
    }
    public void setPruningThresholdIslandWithStops(int pruningThresholdIslandWithStops) {
    	this.pruningThresholdIslandWithStops = pruningThresholdIslandWithStops;
    }

}