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

import java.io.IOException;

import com.fasterxml.jackson.databind.node.NullNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An implementation of GraphSource that store a transient graph in memory.
 */
public class MemoryGraphSource implements GraphSource {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryGraphSource.class);

    private Router router;

    /**
     * Create an in-memory graph source. If the graph contains an embedded runtime-server
     * configuration, it will be used.
     */
    public MemoryGraphSource(String routerId, Graph graph) {
        router = new Router(routerId, graph);
        router.graph.routerId = routerId;
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

        try {
            // There is no on-disk set of files for this graph, so only check for embedded router-config JSON.
            ObjectMapper mapper = new ObjectMapper();
            JsonNode routerJsonConf;
            if (router.graph.routerConfig == null) {
                LOG.info("No embedded router config available");
                routerJsonConf = NullNode.getInstance();
            } else {
                routerJsonConf = mapper.readTree(router.graph.routerConfig);
            }
            router.startup(routerJsonConf);
            return true;
        } catch (IOException e) {
            LOG.error("Can't startup graph: error with embed config (" + router.graph.routerConfig
                    + ")", e);
            return false;
        }
    }

    @Override
    public void evict() {
        if (router != null) {
            router.shutdown();
        }
        router = null;
    }
}
