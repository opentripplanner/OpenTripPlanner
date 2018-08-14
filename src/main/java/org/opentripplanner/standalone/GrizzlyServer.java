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

    private static final int MIN_THREADS = 4;

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
     * OTP is CPU-bound, so we want roughly as many worker threads as we have cores, subject to some constraints.
     */
    private int getMaxThreads() {
        int maxThreads = Runtime.getRuntime().availableProcessors();
        LOG.info("Java reports that this machine has {} available processors.", maxThreads);
        // Testing shows increased throughput up to 1.25x as many threads as cores
        maxThreads *= 1.25;
        if (params.maxThreads != null) {
            maxThreads = params.maxThreads;
            LOG.info("Based on configuration, forced max thread pool size to {} threads.", maxThreads);
        }
        if (maxThreads < MIN_THREADS) {
            // Some machines apparently report 1 processor even when they have 8.
            maxThreads = MIN_THREADS;
        }
        LOG.info("Maximum HTTP handler thread pool size will be {} threads.", maxThreads);
        return maxThreads;
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

        // Set up a pool of threads to handle incoming HTTP requests.
        // According to the Grizzly docs, setting the core and max pool size equal with no queue limit
        // will use a more efficient fixed-size thread pool implementation.
        // TODO we should probably use Grizzly async processing rather than tying up the HTTP handler threads.
        int nHandlerThreads = getMaxThreads();
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
            .setCorePoolSize(nHandlerThreads)
            .setMaxPoolSize(nHandlerThreads)
            .setQueueLimit(-1);

        /* HTTP (non-encrypted) listener */
        NetworkListener httpListener = new NetworkListener("otp_insecure", params.bindAddress, params.port);
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

        /* 3. A static content handler to serve local files from the filesystem, under the "local" path. */
        if (params.clientDirectory != null) {
            StaticHttpHandler localHandler = new StaticHttpHandler(
                    params.clientDirectory.getAbsolutePath());
            localHandler.setFileCacheEnabled(false);
            httpServer.getServerConfiguration().addHttpHandler(localHandler, "/local");
        }

        /* 3. Test alternate HTTP handling without Jersey. */
        // As in servlets, * is needed in base path to identify the "rest" of the path.
        // GraphService gs = (GraphService) iocFactory.getComponentProvider(GraphService.class).getInstance();
        // Graph graph = gs.getGraph();
        // httpServer.getServerConfiguration().addHttpHandler(new OTPHttpHandler(graph), "/test/*");

        // Add shutdown hook to gracefully shut down Grizzly.
        // Signal handling (sun.misc.Signal) is potentially not available on all JVMs.
        Thread shutdownThread = new Thread(httpServer::shutdown);
        Runtime.getRuntime().addShutdownHook(shutdownThread);

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

        // Clean up graceful shutdown hook before shutting down Grizzly.
        Runtime.getRuntime().removeShutdownHook(shutdownThread);
        httpServer.shutdown();
    }
}
