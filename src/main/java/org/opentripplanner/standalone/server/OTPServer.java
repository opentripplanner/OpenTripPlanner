package org.opentripplanner.standalone.server;

import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.util.WeakCollectionCleaner;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This replaces a Spring application context, which OTP originally used.
 * It contains a field referencing each top-level component of an OTP server. This means that supplying a single
 * instance of this object allows accessing any of the other OTP components.
 * TODO OTP2 refactor: rename to OTPContext or OTPComponents and use to draft injection approach
 */
public class OTPServer {

    private static final Logger LOG = LoggerFactory.getLogger(OTPServer.class);

    public final CommandLineParameters params;

    private final Router router;

    public OTPServer (CommandLineParameters params, Router router) {
        LOG.info("Wiring up and configuring server.");
        this.params = params;
        this.router = router;
    }

    /**
     * Hook to cleanup various stuff of some used libraries (org.geotools), which depend on the
     * external client to call them for cleaning-up.
     */
    private static void cleanupWebapp() {
        LOG.info("Web application shutdown: cleaning various stuff");
        WeakCollectionCleaner.DEFAULT.exit();
        DeferredAuthorityFactory.exit();
    }

    public Router getRouter() {
        return router;
    }

    /**
     * This method is used to create a {@link RoutingService} valid for one request. It grantees
     * that the data and services used are consistent and operate on the same transit snapshot.
     * Any realtime update that happens during the request will not affect the returned service and
     * will not be visible to the request.
     */
    public RoutingService createRoutingRequestService() {
        return new RoutingService(router.graph);
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
    AbstractBinder makeBinder() {
        return new AbstractBinder() {
            @Override
            protected void configure() {
                bind(OTPServer.this).to(OTPServer.class);
            }
        };
    }
}
