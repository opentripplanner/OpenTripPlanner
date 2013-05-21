package org.opentripplanner.api.servlet;

import java.io.IOException;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;

public class GrizzlyServer {

    private static final int PORT = 9090;

    public static void main(String[] args) throws IOException {

        /* LOAD THE SPRING CONTEXT */
        System.out.println("Loading Spring context...");
        GenericApplicationContext actx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(actx);
        String[] contexts = {
            "data-sources.xml",
            // "org/opentripplanner/api/security-application-context.xml"
        };
        for (String contextFile : contexts) {
            xmlReader.loadBeanDefinitions(new ClassPathResource(contextFile));
        }
        actx.refresh();
        actx.registerShutdownHook();

        /* CONFIGURE GRIZZLY SERVER */
        System.out.println("Starting grizzly...");
        // Rather than use Jersey's GrizzlyServerFactory we will construct it manually, so we can
        // set the number of threads, etc.
        HttpServer httpServer = new HttpServer();
        NetworkListener networkListener = new NetworkListener("sample-listener", "localhost", PORT);
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(2).setMaxPoolSize(4);
        networkListener.getTransport().setWorkerThreadPoolConfig(threadPoolConfig);
        httpServer.addListener(networkListener);
        ResourceConfig rc = new PackagesResourceConfig("org.opentripplanner");
        // DelegatingFilterProxy.class.getName() does not seem to work out of the box.
        // Register a custom authentication filter.
        rc.getProperties().put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, 
                 new String[] { GrizzlyAuthFilter.class.getName() });
        // Provide Jersey a factory class that gets injected objects from the Spring context
        IoCComponentProviderFactory ioc_factory = new SpringComponentProviderFactory(rc, actx);

        /* ADD A COUPLE OF HANDLERS (~SERVLETS) */
        // 1. A Grizzly wrapper around the Jersey WebApplication. 
        //    We cannot set the context path to /opentripplanner-api-webapp/ws
        //    https://java.net/jira/browse/GRIZZLY-1481?focusedCommentId=360385&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_360385
        HttpHandler handler = ContainerFactory.createContainer(HttpHandler.class, rc, ioc_factory);
        httpServer.getServerConfiguration().addHttpHandler(handler, "/ws/");
        // 2. A static content server for the client JS apps etc.
        //    This is a filesystem path, not classpath.
        //    Files are relative to the project dir, so
        //    from ./ we can reach e.g. target/classes/data-sources.xml
        httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler("./"), "/cp/");
        
        /* RELINQUISH CONTROL TO THE SERVER THREAD */
        try {
            httpServer.start(); 
            System.out.println("Grizzly server running.");
            Thread.currentThread().join();
        } catch (InterruptedException ie) {
            httpServer.stop();
        }
        
    }
}