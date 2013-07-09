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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphBuilderTask implements Runnable {
    
    private static Logger LOG = LoggerFactory.getLogger(GraphBuilderTask.class); 

    private List<GraphBuilder> _graphBuilders = new ArrayList<GraphBuilder>();

    private File graphFile;
    
    private boolean _alwaysRebuild = true;

    private List<RoutingRequest> _modeList;
    
    private String _baseGraph = null;
    
    private Graph graph = new Graph();

    public void addGraphBuilder(GraphBuilder loader) {
        _graphBuilders.add(loader);
    }

    public void setGraphBuilders(List<GraphBuilder> graphLoaders) {
        _graphBuilders = graphLoaders;
    }

    public void setAlwaysRebuild(boolean alwaysRebuild) {
        _alwaysRebuild = alwaysRebuild;
    }
    
    public void setBaseGraph(String baseGraph) {
        this._baseGraph = baseGraph;
        try {
            graph = Graph.load(new File(baseGraph), LoadLevel.FULL);
        } catch (Exception e) {
            throw new RuntimeException("error loading base graph");
        }
    }

    public void addMode(RoutingRequest mo) {
        _modeList.add(mo);
    }

    public void setModes(List<RoutingRequest> modeList) {
        _modeList = modeList;
    }
    
    public void setPath (String path) {
        graphFile = new File(path.concat("/Graph.obj"));
    }
    
    public Graph getGraph() {
        return this.graph;
    }

    public void run() {
        
        if (graphFile == null) {
            throw new RuntimeException("graphBuilderTask has no attribute graphFile.");
        }

        if( graphFile.exists() && ! _alwaysRebuild) {
            LOG.info("graph already exists and alwaysRebuild=false => skipping graph build");
            return;
        }

        try {
            if (!graphFile.getParentFile().exists())
                if (!graphFile.getParentFile().mkdirs())
                    LOG.error("Failed to create directories for graph bundle at " + graphFile);
            graphFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create or overwrite graph at path " + graphFile);
        }

        //check prerequisites
        ArrayList<String> provided = new ArrayList<String>();
        boolean bad = false;
        for (GraphBuilder builder : _graphBuilders) {
            List<String> prerequisites = builder.getPrerequisites();
            for (String prereq : prerequisites) {
                if (!provided.contains(prereq)) {
                    LOG.error("Graph builder " + builder + " requires " + prereq + " but no previous stages provide it");
                    bad = true;
                }
            }
            provided.addAll(builder.provides());
        }
        if (_baseGraph != null)
            LOG.warn("base graph loaded, not enforcing prerequisites");
        else if (bad)
            throw new RuntimeException("Prerequisites unsatisfied");

        //check inputs
        for (GraphBuilder builder : _graphBuilders) {
            builder.checkInputs();
        }
        
        HashMap<Class<?>, Object> extra = new HashMap<Class<?>, Object>();
        for (GraphBuilder load : _graphBuilders)
            load.buildGraph(graph, extra);

        graph.summarizeBuilderAnnotations();
        try {
            graph.save(graphFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
