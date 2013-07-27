package org.opentripplanner.standalone;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;

public class GrizzlyServer {

    private static final Logger LOG = LoggerFactory.getLogger(GrizzlyServer.class);
    
    static {
        // Remove existing handlers attached to the j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
        // Bridge j.u.l (used by Jersey) to the SLF4J root logger
        SLF4JBridgeHandler.install();
    }
    
    /** A factory that hands Jersey OTP modules to inject. */
    private IoCComponentProviderFactory iocFactory;
    
    /** The command line parameters, including things like port number and content directories. */
    private CommandLineParameters params;

    /** Construct a Grizzly server with the given IoC injector and command line parameters. */
    public GrizzlyServer (OTPComponentProviderFactory cpf, CommandLineParameters params) {
        this.iocFactory = cpf;
        this.params = params;
    }

    public static final String CLIENT_WAR_FILENAME = "client.war";
    
    private static class ClientWarSupplier implements InputSupplier<InputStream> {
        @Override public InputStream getInput() throws IOException {
            InputStream istream =  ClassLoader.getSystemResourceAsStream(CLIENT_WAR_FILENAME);
            if (istream == null) throw new IOException("Cannot find client WAR.");
            return istream;
        }
    }

    /** 
     * Create an HttpHandler to serve up static content from a client WAR inside the OTP JAR.
     * Zip files have the file index at the /end/, so they cannot be properly decoded as a stream.
     * Zip streams do not offer random access, and ZipFiles are considered preferable in all cases.
     * Grizzly does include a static file server but it does not work on resources within a JAR, 
     * only files. Solution: copy the ZIP outside the JAR and expand it in a temp directory.
     * JVM does cache classpath resource streams, but let's just do it manually.
     * Interestingly this even works in Eclipse. 
     * The WAR artifact copying and expansion is still working.    
     */
    public HttpHandler makeClientStaticHandler () {
        File tempDir = Files.createTempDir();
        File clientWar = new File(tempDir, CLIENT_WAR_FILENAME);
        try {
            Files.copy(new ClientWarSupplier(), clientWar);
            ZipFile zip = new ZipFile(clientWar);
            zip.extractAll(tempDir.toString());
        } catch (ZipException e) {
            LOG.error("Error expanding client WAR, client will not be available.");
        } catch (IOException e) {
            LOG.error("Error copying client WAR, client will not be available.");
        }
        return new StaticHttpHandler(tempDir.toString());
    }
    
    public void run() {
        
        /* Rather than use Jersey's GrizzlyServerFactory we will construct one manually, so we can
           set the number of threads, etc. */
        LOG.info("Starting OTP Grizzly server on port {} using graphs at {}", params.port, params.graphDirectory);
        HttpServer httpServer = new HttpServer();
        NetworkListener networkListener = 
                new NetworkListener("otp_listener", "localhost", params.port);
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(1).setMaxPoolSize(Runtime.getRuntime().availableProcessors());
        networkListener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
        httpServer.addListener(networkListener);
        ResourceConfig rc = new PackagesResourceConfig("org.opentripplanner");
        /* DelegatingFilterProxy.class.getName() does not seem to work out of the box.
           Register a custom authentication filter, a filter that removes the /ws/ from OTP
           REST API calls, and a filter that wraps JSON in method calls as needed. */
        rc.getProperties().put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, 
                new String[] { AuthFilter.class.getName(), RewriteFilter.class.getName() });
        rc.getProperties().put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, 
                new String[] { JsonpFilter.class.getName() });

        /* ADD A COUPLE OF HANDLERS (~= SERVLETS) */
        /* 1. A Grizzly wrapper around the Jersey WebApplication. 
              We cannot set the context path to /opentripplanner-api-webapp/ws
              https://java.net/jira/browse/GRIZZLY-1481?focusedCommentId=360385&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_360385 */
        HttpHandler handler = ContainerFactory.createContainer(HttpHandler.class, rc, iocFactory);
        httpServer.getServerConfiguration().addHttpHandler(handler, "/opentripplanner-api-webapp/");
        /* 2. A static content server for the client JS apps etc.
              This is a filesystem path, not classpath. Files are relative to the project dir, so
              from ./ we can reach e.g. target/classes/data-sources.xml */
        HttpHandler staticHandler = makeClientStaticHandler();
        httpServer.getServerConfiguration().addHttpHandler(staticHandler, "/");
        
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
            httpServer.stop();
        }
        
    }
        
}

