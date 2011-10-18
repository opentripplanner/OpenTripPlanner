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

package org.opentripplanner.graph_builder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.impl.GraphSerializationLibrary;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class GraphBuilderTask implements Runnable {
    
    private static Logger _log = LoggerFactory.getLogger(GraphBuilderTask.class); 

    private GraphService _graphService;

    private List<GraphBuilder> _graphBuilders = new ArrayList<GraphBuilder>();

    private GraphBundle _graphBundle;
    
    private boolean _alwaysRebuild = true;

    private List<TraverseOptions> _modeList;

    private double _contractionFactor = 1.0;

    @Autowired
    public void setGraphService(GraphService graphService) {
        _graphService = graphService;
    }
    
    public void addGraphBuilder(GraphBuilder loader) {
        _graphBuilders.add(loader);
    }

    public void setGraphBuilders(List<GraphBuilder> graphLoaders) {
        _graphBuilders = graphLoaders;
    }

    public void setGraphBundle(GraphBundle graphBundle) {
        _graphBundle = graphBundle;
    }
    
    public void setAlwaysRebuild(boolean alwaysRebuild) {
        _alwaysRebuild = alwaysRebuild;
    }
    
    public void addMode(TraverseOptions mo) {
        _modeList.add(mo);
    }

    public void setModes(List<TraverseOptions> modeList) {
        _modeList = modeList;
    }

    public void setContractionFactor(double contractionFactor) {
        _contractionFactor = contractionFactor;
    }

    public void run() {
        
        Graph graph = _graphService.getGraph();

        if (_graphBundle == null) {
            throw new RuntimeException("graphBuilderTask has no attribute graphBundle.");
        }
        graph.setBundle(_graphBundle);

        File graphPath = _graphBundle.getGraphPath();
        
        if( graphPath.exists() && ! _alwaysRebuild) {
            _log.info("graph already exists and alwaysRebuild=false => skipping graph build");
            return;
        }
        
        for (GraphBuilder load : _graphBuilders)
            load.buildGraph(graph);
        
        ContractionHierarchySet chs = new ContractionHierarchySet(graph, _modeList, _contractionFactor);
        chs.build();
        try {
            GraphSerializationLibrary.writeGraph(chs, graphPath);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        
        _graphService.refreshGraph();
    }
}
