package org.opentripplanner.standalone;

import java.io.IOException;
import java.net.BindException;

import lombok.Setter;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;

public class GrizzlyServer {

    private static final Logger LOG = LoggerFactory.getLogger(GrizzlyServer.class);

    @Setter private int port = 8080;
    @Setter private String graphDirectory = "/var/otp/graphs/";
    @Setter private String staticContentDirectory = "./opentripplanner-leaflet-client/src/main/webapp/";
    @Setter private String defaultRouterId = "";

    public void start(String[] args) {
        /* CONFIGURE LOGGING */
        // Remove existing handlers attached to the j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
        // Bridge j.u.l (used by Jersey) to the SLF4J root logger
        SLF4JBridgeHandler.install();
        
        /* CONFIGURE GRIZZLY SERVER */
        LOG.info("Starting OTP Grizzly server...");
        if (args.length > 2)
            port = Integer.parseInt(args[2]);
        // Rather than use Jersey's GrizzlyServerFactory we will construct one manually, so we can
        // set the number of threads, etc.
        HttpServer httpServer = new HttpServer();
        NetworkListener networkListener = new NetworkListener("sample-listener", "localhost", port);
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(1).setMaxPoolSize(Runtime.getRuntime().availableProcessors());
        networkListener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
        httpServer.addListener(networkListener);
        ResourceConfig rc = new PackagesResourceConfig("org.opentripplanner");
        // DelegatingFilterProxy.class.getName() does not seem to work out of the box.
        // Register a custom authentication filter, a filter that removes the /ws/ from OTP
        // REST API calls, and a filter that wraps JSON in method calls as needed.
        rc.getProperties().put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, 
                new String[] { AuthFilter.class.getName(), RewriteFilter.class.getName() });
        rc.getProperties().put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, 
                new String[] { JsonpFilter.class.getName() });

        // Make a factory that hands Jersey OTP modules to inject
        IoCComponentProviderFactory ioc_factory = OTPConfigurator.fromCommandLineArguments(args);

        /* ADD A COUPLE OF HANDLERS (~SERVLETS) */
        // 1. A Grizzly wrapper around the Jersey WebApplication. 
        //    We cannot set the context path to /opentripplanner-api-webapp/ws
        //    https://java.net/jira/browse/GRIZZLY-1481?focusedCommentId=360385&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_360385
        HttpHandler handler = ContainerFactory.createContainer(HttpHandler.class, rc, ioc_factory);
        httpServer.getServerConfiguration().addHttpHandler(handler, "/opentripplanner-api-webapp/");
        // 2. A static content server for the client JS apps etc.
        //    This is a filesystem path, not classpath.
        //    Files are relative to the project dir, so
        //    from ./ we can reach e.g. target/classes/data-sources.xml
        staticContentDirectory = "../opentripplanner-leaflet-client/src/main/webapp/";
        httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler(staticContentDirectory), "/");
        
        /* RELINQUISH CONTROL TO THE SERVER THREAD */
        try {
            httpServer.start(); 
            LOG.info("Grizzly server running.");
            Thread.currentThread().join();
        } catch (BindException be) {
            LOG.error("Cannot bind to port {}. Is it already in use?", port);
        } catch (IOException ioe) {
            LOG.error("IO exception while starting server.");
        } catch (InterruptedException ie) {
            httpServer.stop();
        }
        
    }
    
    public static void main(String[] args) {
        (new GrizzlyServer()).start(args);
    }
    
}