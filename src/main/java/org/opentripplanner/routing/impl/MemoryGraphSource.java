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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.standalone.Router;

/**
 * An implementation of GraphSource that store a transient graph in memory.
 */
public class MemoryGraphSource implements GraphSource {

    private Router router;

    private JsonNode config;

    /** Create an in-memory graph source with no runtime router configuration. */
    public MemoryGraphSource(String routerId, Graph graph) {
        this(routerId, graph, MissingNode.getInstance());
    }

    /** Create an in-memory graph source with the specififed runtime router configuration JSON. */
    public MemoryGraphSource(String routerId, Graph graph, JsonNode config) {
        router = new Router(routerId, graph);
        router.graph.routerId = routerId;
        this.config = config;
        // We will start up the router later on (updaters and runtime configuration options)
    }

    @Override
    public Router getRouter() {
        return router;
    }

    @Override
    public boolean reload(boolean force, boolean preEvict) {
        // "Reloading" does not make sense for memory-graph, but we want to support mixing in-memory and file-based graphs.
        // Start up graph updaters and apply runtime configuration options
        // TODO will the updaters be started repeatedly due to reload calls?
        router.startup(config);
        return true;
    }

    @Override
    public void evict() {
        if (router != null) {
            router.shutdown();
        }
        router = null;
    }
}
