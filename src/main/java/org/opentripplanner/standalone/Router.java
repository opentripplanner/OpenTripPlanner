package org.opentripplanner.standalone;

import java.util.prefs.Preferences;

import org.opentripplanner.analyst.request.IsoChroneSPTRenderer;
import org.opentripplanner.analyst.request.Renderer;
import org.opentripplanner.analyst.request.SPTCache;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.TileCache;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.SPTServiceFactory;
import org.opentripplanner.routing.services.PathService;

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
     * @see GraphUpdaterConfigurator
     */
    public interface LifecycleManager {

        /**
         * Startup a router when it has been created.
         * @param router The router to bind/setup
         * @param config The configuration (loaded from Graph.properties for example).
         */
        public void startupRouter(Router router, Preferences config);

        /**
         * Shutdown a router when evicted / (auto-)reloaded. Stop any real-time updaters threads.
         */
        public void shutdownRouter(Router router);
    }

    public String id;
    public Graph graph;

    // Core services
    public PlanGenerator planGenerator;
    public PathService pathService;
    public SPTServiceFactory sptServiceFactory;

    // Inspector/debug services
    public TileRendererManager tileRendererManager;

    // Analyst services
    public SPTCache sptCache;
    public TileCache tileCache;
    public Renderer renderer;
    public IsoChroneSPTRenderer isoChroneSPTRenderer;
    public SampleGridRenderer sampleGridRenderer;

    public Router(String id, Graph graph) {
        this.id = id;
        this.graph = graph;
    }

}
