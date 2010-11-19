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
import org.opentripplanner.routing.contraction.ModeAndOptimize;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.impl.ContractionHierarchySerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class GraphBuilderTask implements Runnable {
    
    private static Logger _log = LoggerFactory.getLogger(GraphBuilderTask.class); 

    private Graph _graph;

    private List<GraphBuilder> _graphBuilders = new ArrayList<GraphBuilder>();

    private GraphBundle _graphBundle;
    
    private boolean _alwaysRebuild = true;

    private List<ModeAndOptimize> _modeList;

    private double _contractionFactor = 1.0;

    @Autowired
    public void setGraph(Graph graph) {
        _graph = graph;
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
    
    public void addMode(ModeAndOptimize mo) {
        _modeList.add(mo);
    }

    public void setModes(List<ModeAndOptimize> modeList) {
        _modeList = modeList;
    }

    public void setContractionFactor(double contractionFactor) {
        _contractionFactor = contractionFactor;
    }

    public void run() {
        
        File graphPath = _graphBundle.getGraphPath();
        
        if( graphPath.exists() && ! _alwaysRebuild) {
            _log.info("graph already exists and alwaysRebuild=false => skipping graph build");
            return;
        }
        
        for (GraphBuilder load : _graphBuilders)
            load.buildGraph(_graph);
        
        ContractionHierarchySet chs = new ContractionHierarchySet(_graph, _modeList, _contractionFactor);
        chs.build();
        try {
            ContractionHierarchySerializationLibrary.writeGraph(chs, graphPath);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
