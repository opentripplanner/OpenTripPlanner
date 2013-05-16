package org.opentripplanner.api.servlet;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;

public class GrizzlyServer {

    private static final int PORT = 9090;
    private static final String URI = "http://localhost/ws/";
    
    public static void main(String[] args) throws IOException {
        
        final URI base_uri = UriBuilder.fromUri(URI).port(PORT).build();
        
        System.out.println("Loading Spring context...");
        GenericApplicationContext gctx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(gctx);
        String[] contexts = {
            "data-sources.xml",
            "org/opentripplanner/api/security-application-context.xml"
        };
        for (String contextFile : contexts) {
            xmlReader.loadBeanDefinitions(new ClassPathResource(contextFile));
        }
        gctx.refresh();
        gctx.registerShutdownHook();

        System.out.println("Starting grizzly...");
        ResourceConfig rc = new PackagesResourceConfig("org.opentripplanner");
        IoCComponentProviderFactory ioc_factory =
                new SpringComponentProviderFactory(rc, gctx);
        
        // The /ws prefix in the servlet came from web.xml
        // Here, the URI context path (subpath) is provided in the first parameter. 
        HttpServer httpServer = GrizzlyServerFactory.createHttpServer(base_uri, rc, ioc_factory);

        /* add another handler (~= servlet) to serve up static content */
        // This is a filesystem path, not classpath. 
        // Files are relative to the project dir, so
        // from ./ we can reach e.g. target/classes/data-sources.xml
        httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler("./"), "/cp/");
        
        try {
            httpServer.start();
            System.out.println("Grizzly server running at " + base_uri);
            System.out.println("WADL at " + base_uri +"application.wadl");
            System.out.println("Test with " + base_uri +"metadata");
            Thread.currentThread().join();
        } catch (InterruptedException ie) {
            httpServer.stop();
        } 
    }
}