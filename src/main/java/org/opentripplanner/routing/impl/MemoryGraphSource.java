/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import java.util.Properties;
import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.updater.PropertiesPreferences;

/**
 * An implementation of GraphSource that store a transient graph in memory.
 * 
 */
public class MemoryGraphSource implements GraphSource {

    private Graph graph;

    private GraphUpdaterConfigurator decorator = new GraphUpdaterConfigurator();

    public MemoryGraphSource(String routerId, Graph graph) {
        this(routerId, graph, new PropertiesPreferences(new Properties()));
    }

    public MemoryGraphSource(String routerId, Graph graph, Preferences config) {
        this.graph = graph;
        this.graph.routerId = routerId;
        decorator.setupGraph(graph, config);
    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public boolean reload(boolean force, boolean preEvict) {
        /*
         * The method does not make sense for memory-graph, but we want to be able to support it if
         * we want to mix in-memory graph with file-based graphs.
         */
        return true;
    }

    @Override
    public void evict() {
        if (graph != null) {
            decorator.shutdownGraph(graph);
        }
        graph = null;
    }
}
