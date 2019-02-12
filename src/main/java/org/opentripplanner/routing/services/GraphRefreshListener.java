package org.opentripplanner.routing.services;

/**
 * Service classes interested in being notified of graph refresh events should implement this class.
 * Implementations of {@link GraphService} will call {@link #handleGraphRefresh(GraphService)} when
 * a graph is refreshed through a call to {@link GraphService#refreshGraph()} or when the graph is
 * initial loaded.
 * 
 * @author bdferris
 * 
 */
public interface GraphRefreshListener {
    public void handleGraphRefresh(GraphService graphService);
}
