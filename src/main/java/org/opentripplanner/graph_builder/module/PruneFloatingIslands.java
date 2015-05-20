/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
    private int islandWithoutStopsMaxSize = 40;

    /**
     * this field indicate the maximum size for island with stops
     * island under this size will be pruned.
     */
    private int islandWithStopsMaxSize = 5;

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
        
        StreetUtils.pruneFloatingIslands(graph, islandWithoutStopsMaxSize, 
                islandWithStopsMaxSize, islandLogFile);
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

}