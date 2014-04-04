package org.opentripplanner.standalone;

import java.io.IOException;
import java.net.BindException;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.server.ContainerFactory;
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
    private OTPServer server;

    /** Construct a Grizzly server with the given IoC injector and command line parameters. */
    public GrizzlyServer (CommandLineParameters params, OTPServer server) {
        this.params = params;
        this.server = server;
    }

    public void run() {
        
        /* The code below does roughly the same steps as Jersey's GrizzlyServerFactory, but we will instead construct
           an HttpServer and NetworkListener manually so we can set the number of threads, etc.  */
        LOG.info("Starting OTP Grizzly server on port {} using graphs at {}", params.port, params.graphDirectory);
        HttpServer httpServer = new HttpServer();

        /* Configure SSL */
        SSLContextConfigurator sslConfig = new SSLContextConfigurator();
        sslConfig.setKeyStoreFile("/var/otp/ssh/keystore_server");
        sslConfig.setKeyStorePass("opentrip");

        /* HTTP (non-encrypted) listener */
        NetworkListener httpListener = new NetworkListener("otp_insecure_listener", "0.0.0.0", params.port); // TODO add option for address to listen on
        // OTP is CPU-bound, we don't want more threads than cores. We should switch to async handling.
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
            .setCorePoolSize(1)
            .setMaxPoolSize(Runtime.getRuntime().availableProcessors());
        httpListener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
        httpListener.setSecure(false);
        httpServer.addListener(httpListener);

        /* HTTPS listener */
        NetworkListener httpsListener = new NetworkListener("otp_secure_listener", "0.0.0.0", params.port + 1);
        // Ideally we'd share the threads between HTTP and HTTPS.
        httpsListener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
        httpsListener.setSecure(true);
        httpsListener.setSSLEngineConfig(
                new SSLEngineConfigurator(sslConfig)
                        .setClientMode(false)
                        .setNeedClientAuth(false)
        );
        httpServer.addListener(httpsListener);

        /* Add a few handlers (~= servlets) to the Grizzly server. */

        /* 1. A Grizzly wrapper around the Jersey Application. */
        Application app = new OTPApplication(server);
        HttpHandler dynamicHandler = ContainerFactory.createContainer(HttpHandler.class, app);
        httpServer.getServerConfiguration().addHttpHandler(dynamicHandler, "/otp");

        /* 2. A static content server for the client JS apps etc. Now using classpath! check http://localhost:8080/maven-version.properties */
        HttpHandler staticHandler = new CLStaticHttpHandler(GrizzlyServer.class.getClassLoader(), "/");
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
