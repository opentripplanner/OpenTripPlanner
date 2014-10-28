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

package org.opentripplanner.routing.services;

import java.util.Collection;

import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;

/**
 * A GraphService maps RouterIds to Graphs.
 * 
 * The service interface allows us to decouple the deserialization, loading, and management of the
 * underlying graph objects from the classes that need access to the objects. This indirection
 * allows us to provide multiple graphs distringuished by routerIds or to dynamically swap in new
 * graphs if underlying data changes.
 */
public interface GraphService {

    /** @param defaultRouterId The ID of the default router to return when no one is specified */
    public void setDefaultRouterId(String defaultRouterId);

    /** @return the current default graph object */
    public Graph getGraph() throws GraphNotFoundException;

    /** @return the graph object for the given router ID */
    public Graph getGraph(String routerId) throws GraphNotFoundException;

    /** @return a collection of all valid router IDs for this server */
    public Collection<String> getRouterIds();

    /**
     * Blocking method to associate the specified router ID with the corresponding graph file on
     * disk, load that serialized graph, and enable its use in routing. The relationship between
     * router IDs and paths in the filesystem is determined by the graphService implementation.
     * 
     * @param routerId
     * @param preEvict
     * 
     * @return whether the operation completed successfully
     */
    public boolean registerGraph(String routerId, GraphSource graphSource);

    /**
     * Reload all registered graphs from wherever they came from.
     * 
     * @param preEvict When true, release the existing graph (if any) before loading. This will
     *        halve the amount of memory needed for the operation, but routing will be unavailable
     *        for that graph during the load process
     * @return whether the operation completed successfully
     */
    public boolean reloadGraphs(boolean preEvict);

    /**
     * Dissociate a router ID from the corresponding graph object, and disable that router ID for
     * use in routing.
     * 
     * @return whether a graph was associated with this router ID and was evicted.
     */
    public boolean evictGraph(String routerId);

    /**
     * Dissocate all graphs from their router IDs and release references to the graphs to allow
     * garbage collection. Routing will not be possible until new graphs are registered.
     * 
     * This is equivalent to calling evictGraph on every registered router ID.
     */
    public int evictAll();

    /**
     * @return The default GraphSource factory. Needed in case someone want to register or save a new
     *         router with a router ID only (namely, via the web-service API).
     */
    public GraphSource.Factory getGraphSourceFactory();
}
