package org.opentripplanner.standalone;

import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.analyst.request.IsoChroneSPTRenderer;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;

/**
 * Represents the configuration of a single router (a single graph for a specific geographic area)
 * in an OTP server.
 */
public class Router {

    /**
     * A manager responsible for setting-up and shutting-down a Router.
     * 
     * Current responsibility are: 1) Binding proper services (depending on configuration, such as
     * command-line or properties file, etc...) and 2) starting / stopping real-time updaters
     * (delegated to the GraphUpdaterConfigurator class).
     * 
     * @see org.opentripplanner.updater.GraphUpdaterConfigurator
     */
    public interface LifecycleManager {

        /**
         * Startup a router when it has been created.
         * @param router The router to bind/setup
         * @param config The configuration (loaded from Graph.properties for example).
         */
        public void startupRouter(Router router, JsonNode config);

        /**
         * Shutdown a router when evicted / (auto-)reloaded. Stop any real-time updaters threads.
         */
        public void shutdownRouter(Router router);
    }

    public String id;
    public Graph graph;

    /* TODO The fields for "components" are slowly disappearing... maybe at some point a router will be nothing but configuration values tied to a Graph. */

    // Inspector/debug services
    public TileRendererManager tileRendererManager;

    // Analyst services
    public TileCache tileCache;
    public Renderer renderer;
    public IsoChroneSPTRenderer isoChroneSPTRenderer;
    public SampleGridRenderer sampleGridRenderer;

    // A RoutingRequest containing default parameters that will be cloned when handling each request
    public RoutingRequest defaultRoutingRequest;

    public Router(String id, Graph graph) {
        this.id = id;
        this.graph = graph;
    }

}
