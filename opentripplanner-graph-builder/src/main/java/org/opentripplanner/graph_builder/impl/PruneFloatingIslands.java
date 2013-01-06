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

package org.opentripplanner.graph_builder.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.*;
import org.apache.log4j.Logger;
import org.opentripplanner.common.StreetUtils;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.slf4j.*;

public class PruneFloatingIslands implements GraphBuilder {

    private static org.slf4j.Logger _log = LoggerFactory.getLogger(PruneFloatingIslands.class);

    @Setter
    private int maxIslandSize = 40;

    @Setter
    private int islandWithStopMaxSize = 5;

    @Setter
    private String islandLogFile = "";

    @Setter
    private TransitToStreetNetworkGraphBuilderImpl transitToStreetNetwork;

    public List<String> provides() {
        return Collections.emptyList();
    }

    public List<String> getPrerequisites() {
//        return Arrays.asList("streets","linking");
        return Arrays.asList("streets");
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        _log.warn("Pruning isolated islands ...");
        StreetUtils.pruneFloatingIslands(graph, maxIslandSize, islandWithStopMaxSize,
                LoggerAppenderProvider.createCsvFile4LoggerCat(islandLogFile, "islands"));
        if(transitToStreetNetwork == null){
            _log.warn("Could not reconnect stop, TransitToStreetNetworkGraphBuilder was not provided");
        }else{
            //reconnect stops on small islands (that removed)
            transitToStreetNetwork.buildGraph(graph,extra);
        }
        _log.warn("Done pruning isolated islands");
    }

    @Override
    public void checkInputs() {
        //no inputs
    }

}