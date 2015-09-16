package org.opentripplanner.standalone;

import java.io.File;
import java.io.IOException;
import java.net.BindException;

import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
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

    /**
     * This function goes through roughly the same steps as Jersey's GrizzlyServerFactory, but we instead construct
     * an HttpServer and NetworkListener manually so we can set the number of threads and other details.
     */
    public void run() {
        
        LOG.info("Starting OTP Grizzly server on ports {} (HTTP) and {} (HTTPS) of interface {}",
            params.port, params.securePort, params.bindAddress);
        LOG.info("OTP server base path is {}", params.basePath);
        HttpServer httpServer = new HttpServer();

        /* Configure SSL */
        SSLContextConfigurator sslConfig = new SSLContextConfigurator();
        sslConfig.setKeyStoreFile(new File(params.basePath, "keystore").getAbsolutePath());
        sslConfig.setKeyStorePass("opentrip");

        /* OTP is CPU-bound, so we want only as many worker threads as we have cores. */
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
            .setCorePoolSize(1)
            .setMaxPoolSize(Runtime.getRuntime().availableProcessors());

        /* HTTP (non-encrypted) listener */
        NetworkListener httpListener = new NetworkListener("otp_insecure", params.bindAddress, params.port);
        // OTP is CPU-bound, we don't want more threads than cores. TODO: We should switch to async handling.
        httpListener.setSecure(false);

        /* HTTPS listener */
        NetworkListener httpsListener = new NetworkListener("otp_secure", params.bindAddress, params.securePort);
        // Ideally we'd share the threads between HTTP and HTTPS.
        httpsListener.setSecure(true);
        httpsListener.setSSLEngineConfig(
                new SSLEngineConfigurator(sslConfig)
                        .setClientMode(false)
                        .setNeedClientAuth(false)
        );

        // For both HTTP and HTTPS listeners: enable gzip compression, set thread pool, add listener to httpServer.
        for (NetworkListener listener : new NetworkListener[] {httpListener, httpsListener}) {
            CompressionConfig cc = listener.getCompressionConfig();
            cc.setCompressionMode(CompressionConfig.CompressionMode.ON);
            cc.setCompressionMinSize(50000); // the min number of bytes to compress
            cc.setCompressableMimeTypes("application/json", "text/json"); // the mime types to compress
            listener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
            httpServer.addListener(listener);
        }

        /* Add a few handlers (~= servlets) to the Grizzly server. */

        /* 1. A Grizzly wrapper around the Jersey Application. */
        Application app = new OTPApplication(server, !params.insecure);
        HttpHandler dynamicHandler = ContainerFactory.createContainer(HttpHandler.class, app);
        httpServer.getServerConfiguration().addHttpHandler(dynamicHandler, "/otp/");

        /* 2. A static content handler to serve the client JS apps etc. from the classpath. */
        CLStaticHttpHandler staticHandler = new CLStaticHttpHandler(GrizzlyServer.class.getClassLoader(), "/client/");
        if (params.disableFileCache) {
            LOG.info("Disabling HTTP server static file cache.");
            staticHandler.setFileCacheEnabled(false);
        }
        httpServer.getServerConfiguration().addHttpHandler(staticHandler, "/");

        /*
         * 3. A static content handler to serve local files from the filesystem, under the "local"
         * path.
         */
        if (params.clientDirectory != null) {
            StaticHttpHandler localHandler = new StaticHttpHandler(
                    params.clientDirectory.getAbsolutePath());
            localHandler.setFileCacheEnabled(false);
            httpServer.getServerConfiguration().addHttpHandler(localHandler, "/local");
        }

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
