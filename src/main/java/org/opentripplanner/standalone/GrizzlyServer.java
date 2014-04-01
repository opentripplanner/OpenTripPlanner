package org.opentripplanner.standalone;

import java.io.IOException;
import java.net.BindException;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.opentripplanner.api.OTPHttpHandler;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.core.Application;

public class GrizzlyServer {

    private static final Logger LOG = LoggerFactory.getLogger(GrizzlyServer.class);
    
    static {
        // Remove existing handlers attached to the j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
        // Bridge j.u.l (used by Jersey) to the SLF4J root logger
        SLF4JBridgeHandler.install();
    }

    /** The command line parameters, including things like port number and content directories. */
    private CommandLineParameters params;

    /** Construct a Grizzly server with the given IoC injector and command line parameters. */
    public GrizzlyServer (CommandLineParameters params) {
        this.params = params;
    }

    public void run() {
        
        /* Rather than use Jersey's GrizzlyServerFactory we will construct one manually, so we can
           set the number of threads, etc. The code below does roughly the same steps as GrizzlyServerFactory. */
        LOG.info("Starting OTP Grizzly server on port {} using graphs at {}", params.port, params.graphDirectory);
        HttpServer httpServer = new HttpServer();
        // TODO add option for address to listen on
        NetworkListener networkListener = new NetworkListener("otp_listener", "0.0.0.0", params.port);
        // OTP is CPU-bound!
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(1).setMaxPoolSize(Runtime.getRuntime().availableProcessors());
        networkListener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
        httpServer.addListener(networkListener);

        /* Application is the JAX-RS way to configure a web app.
           ResourceConfig is a Jersey-specific subclass of Application for package scanning etc. */
        Application app = new OTPApplication();

        /* Add a couple of handlers (~= servlets) to the Grizzly server. */

        /* 1. A Grizzly wrapper around the Jersey Application. */
        HttpHandler dynamicHandler = ContainerFactory.createContainer(HttpHandler.class, app);
        httpServer.getServerConfiguration().addHttpHandler(dynamicHandler, "/otp");

        /* 2. A static content server for the client JS apps etc. Now using classpath! */
        HttpHandler staticHandler = new CLStaticHttpHandler(GrizzlyServer.class.getClassLoader());
        httpServer.getServerConfiguration().addHttpHandler(staticHandler, "/");

        /* 3. Test alternate method (no Jersey). */
        // As in servlets, * is needed in base path to identify the "rest" of the path.
        // GraphService gs = (GraphService) iocFactory.getComponentProvider(GraphService.class).getInstance();
        // Graph graph = gs.getGraph();
        // httpServer.getServerConfiguration().addHttpHandler(new OTPHttpHandler(graph), "/test/*");
        
        /* RELINQUISH CONTROL TO THE SERVER THREAD */
        try {
            httpServer.start(); 
            LOG.info("Grizzly server running.");
            Thread.currentThread().join();
        } catch (BindException be) {
            LOG.error("Cannot bind to port {}. Is it already in use?", params.port);
        } catch (IOException ioe) {
            LOG.error("IO exception while starting server.");
        } catch (InterruptedException ie) {
            LOG.info("Interrupted, shutting down.");
        }
        httpServer.shutdown();

    }
}
