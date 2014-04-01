package org.opentripplanner.standalone;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.opentripplanner.api.model.OTPObjectMapperProvider;
import org.opentripplanner.api.resource.AlertPatcher;
import org.opentripplanner.api.resource.BikeRental;
import org.opentripplanner.api.resource.GeocoderResource;
import org.opentripplanner.api.resource.Metadata;
import org.opentripplanner.api.resource.Planner;
import org.opentripplanner.api.resource.ProfileEndpoint;
import org.opentripplanner.api.resource.Routers;
import org.opentripplanner.api.resource.ServerInfo;
import org.opentripplanner.api.resource.analyst.LIsochrone;
import org.opentripplanner.api.resource.analyst.LegendResource;
import org.opentripplanner.api.resource.analyst.Raster;
import org.opentripplanner.api.resource.analyst.SIsochrone;
import org.opentripplanner.api.resource.analyst.SimpleIsochrone;
import org.opentripplanner.api.resource.analyst.TileService;
import org.opentripplanner.api.resource.analyst.TimeGridWs;
import org.opentripplanner.api.resource.analyst.WebMapService;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.core.Application;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * A Jersey Application subclass which provides hard-wired configuration of an OTP server. Avoids injection of any kind.
 */
public class OTPApplication extends Application {

    public final OTPServer server;

    public OTPApplication (OTPServer server) {
        this.server = server;
    }

    /**
     * This method tells Jersey which classes define web resources, and which features it should enable.
     */
    @Override
    public Set<Class<?>> getClasses() {
        return Sets.newHashSet(
            // Jersey Web resource definition classes
            Planner.class,
            SimpleIsochrone.class,
            TileService.class,
            BikeRental.class,
            LIsochrone.class,
            GeocoderResource.class,
            TimeGridWs.class,
            WebMapService.class,
            AlertPatcher.class,
            Planner.class,
            SIsochrone.class,
            Routers.class,
            Raster.class,
            LegendResource.class,
            Metadata.class,
            ProfileEndpoint.class,
            SimpleIsochrone.class,
            ServerInfo.class,
            // Enable Jackson JSON and XML serialization
            OTPObjectMapperProvider.class,
            // Filters -- confirmed, these are picked up by Jersey.
            AuthFilter.class,
            JsonpFilter.class
        );
        // JerseyInjector
        // replace with simple Parameter classes that have String constructors.

    }

    @Override
    public Map<String, Object> getProperties() {
        Map props = Maps.newHashMap();
        props.put(ServerProperties.TRACING, Boolean.TRUE);
        /* Register a custom authentication filter and a filter that wraps JSON in method calls as needed. */
        // props.put(org.glassfish.jersey.server PROPERTY_CONTAINER_REQUEST_FILTERS, Arrays.asList());
        // props.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, Arrays.asList(JsonpFilter.class));
        return props;
    }

    static {
        // Remove existing handlers attached to the j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        // Bridge j.u.l (used by Jersey) to the SLF4J root logger
        SLF4JBridgeHandler.install();
    }

}
