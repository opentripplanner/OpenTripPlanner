package org.opentripplanner.standalone;

import java.io.File;
import java.util.Collection;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.opentripplanner.analyst.DiskBackedPointSetCache;
import org.opentripplanner.analyst.PointSetCache;
import org.opentripplanner.analyst.SurfaceCache;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.scripting.impl.ScriptingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is essentially replacing a Spring application context.
 * It just bundles together references to all the OTP components so we can pass them around the system.
 */
public class OTPServer {

    private static final Logger LOG = LoggerFactory.getLogger(OTPServer.class);

    // Core OTP modules
    private GraphService graphService;

    /** The directory under which graphs, caches, etc. will be stored. */
    public File basePath = null;

    // Optional Analyst global modules (caches)
    public SurfaceCache surfaceCache;
    public PointSetCache pointSetCache;
    public ScriptingService scriptingService;

    public CommandLineParameters params;

    public OTPServer (CommandLineParameters params, GraphService gs) {
        LOG.info("Wiring up and configuring server.");

        this.params = params;

        // Core OTP modules
        this.graphService = gs;

        // Optional Analyst Modules.
        if (params.analyst) {
            surfaceCache = new SurfaceCache(30);
            pointSetCache = new DiskBackedPointSetCache(100, params.pointSetDirectory);
        }

        scriptingService = new ScriptingService(this);
        scriptingService.enableScriptingWebService = params.enableScriptingWebService;
        if (params.enableScriptingWebService) {
            LOG.warn("WARNING: scripting web-service is activated. For public-facing server this is a SERIOUS SECURITY RISK!");
        }
    }

    /**
     * @return The GraphService. Please use it only when the GraphService itself is necessary. To
     *         get Graph instances, use getRouter().
     */
    public GraphService getGraphService() {
        return graphService;
    }

    /**
     * @return A list of all router IDs currently available.
     */
    public Collection<String> getRouterIds() {
        return graphService.getRouterIds();
    }

    public Router getRouter(String routerId) throws GraphNotFoundException {
        return graphService.getRouter(routerId);
    }

    /**
     * Return an HK2 Binder that injects this specific OTPServer instance into Jersey web resources.
     * This should be registered in the ResourceConfig (Jersey) or Application (JAX-RS) as a singleton.
     * Jersey forces us to use injection to get application context into HTTP method handlers, but in OTP we always
     * just inject this OTPServer instance and grab anything else we need (routers, graphs, application components)
     * from this single object.
     *
     * More on custom injection in Jersey 2:
     * http://jersey.576304.n2.nabble.com/Custom-providers-in-Jersey-2-tp7580699p7580715.html
     */
     public AbstractBinder makeBinder() {
        return new AbstractBinder() {
            @Override
            protected void configure() {
                bind(OTPServer.this).to(OTPServer.class);
            }
        };
    }

}
