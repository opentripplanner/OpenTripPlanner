package org.opentripplanner.routing.services;

import java.io.InputStream;

import org.opentripplanner.standalone.Router;

/**
 * A class responsible of graph creation / ownership.
 * 
 */
public interface GraphSource {

    /**
     * Factory of GraphSource. It's used for the Jersey API to be able to map GraphSource to
     * external routerID, for operations such as registering new graph, or saving graph data from
     * binary data upload. If the API need to perform other type of operation one can replace the
     * default factory in GraphService.
     * 
     * @see GraphService
     */
    public interface Factory {

        /**
         * @param routerId Id of the router.
         * @return a new GraphSource for the given routerId.
         */
        public GraphSource createGraphSource(String routerId);

        /**
         * Save the graph data, but don't load it in memory. The file location is based on the
         * router id. If the graph already exists, the graph will be overwritten. The relationship
         * between router IDs and paths in the filesystem is determined by the graphService
         * implementation.
         * 
         * @param routerId the routerId of the graph
         * @param is graph data as input stream
         * @return true if the operation succedded, false otherwise (will catch IOExceptions).
         */
        public boolean save(String routerId, InputStream is);
    }

    /**
     * @return The router containing a graph object. Delegates to the Router lifecycle manager the
     *         startup and shutdown of the graph.
     */
    public Router getRouter();

    /**
     * Reload the graph from it's source.
     * 
     * @param force True to force a reload, false to check only.
     * @param preEvict True to evict the old version *before* loading the new one. In that case the
     *        implementation have to take care of making the getGraph() call wait while the new
     *        graph is being loaded and not return null.
     * @return False if a new graph has not been reloaded and we could not keep the previous one: it
     *         should be evicted.
     */
    public boolean reload(boolean force, boolean preEvict);

    /**
     * Callback when the graph (source) gets evicted from the repository.
     */
    public void evict();
}
