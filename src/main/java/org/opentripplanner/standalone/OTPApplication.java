package org.opentripplanner.standalone;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.opentripplanner.api.common.OTPExceptionMapper;
import org.opentripplanner.api.model.JSONObjectMapperProvider;
import org.opentripplanner.api.resource.AlertPatcher;
import org.opentripplanner.api.resource.BikeRental;
import org.opentripplanner.api.resource.ExternalGeocoderResource;
import org.opentripplanner.api.resource.Metadata;
import org.opentripplanner.api.resource.Planner;
import org.opentripplanner.api.resource.PointSetResource;
import org.opentripplanner.api.resource.ProfileResource;
import org.opentripplanner.api.resource.Routers;
import org.opentripplanner.api.resource.ServerInfo;
import org.opentripplanner.api.resource.LIsochrone;
import org.opentripplanner.api.resource.LegendResource;
import org.opentripplanner.api.resource.Raster;
import org.opentripplanner.api.resource.SIsochrone;
import org.opentripplanner.api.resource.SimpleIsochrone;
import org.opentripplanner.api.resource.SurfaceResource;
import org.opentripplanner.api.resource.TileService;
import org.opentripplanner.api.resource.WebMapService;
import org.opentripplanner.api.resource.TimeGridWs;
import org.opentripplanner.index.GeocoderResource;
import org.opentripplanner.index.IndexAPI;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.core.Application;
import java.util.Map;
import java.util.Set;

/**
 * A JAX-RS Application subclass which provides hard-wired configuration of an OTP server.
 * Avoids auto-scanning of any kind, and keeps injection to a bare minimum using HK2, the injection
 * library Jersey itself uses.
 *
 * Jersey has its own ResourceConfig class which is a subclass of Application.
 * We can get away with not using any Jersey-specific "conveniences" and stick with stock JAX-RS.
 */
public class OTPApplication extends Application {

    static {
        // Remove existing handlers attached to the j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        // Bridge j.u.l (used by Jersey) to the SLF4J root logger
        SLF4JBridgeHandler.install();
    }

    public final OTPServer server;

    /**
     * The OTPServer provides entry points to OTP routing functionality for a collection of OTPRouters.
     * It provides a Java API, not an HTTP API.
     * The OTPApplication wraps an OTPServer in a Jersey (JAX-RS) Application, configuring an HTTP API.
     */
    public OTPApplication (OTPServer server) {
        this.server = server;
    }

    /**
     * This method registers classes with Jersey to define web resources and enable custom features.
     * These are classes (not instances) that will be instantiated by Jersey for each request (they are request-scoped).
     * Types that have been confirmed to work are: annotated resources, ContextResolver<ObjectMapper> implementation,
     * ContainerResponseFilter and ContainerRequestFilter.
     * Note that the listed classes do not need to be annotated with @Provider -- that is for scanning config.
     */
    @Override
    public Set<Class<?>> getClasses() {
        return Sets.newHashSet(
            /* Jersey resource classes: define web services, i.e. an HTTP API. */
            Planner.class,
            IndexAPI.class,
            ExternalGeocoderResource.class,
            GeocoderResource.class,
            SimpleIsochrone.class,
            TileService.class,
            BikeRental.class,
            LIsochrone.class,
            ExternalGeocoderResource.class,
            TimeGridWs.class,
            WebMapService.class,
            AlertPatcher.class,
            Planner.class,
            SIsochrone.class,
            Routers.class,
            Raster.class,
            LegendResource.class,
            Metadata.class,
            ProfileResource.class,
            SimpleIsochrone.class,
            ServerInfo.class,
            SurfaceResource.class,
            PointSetResource.class,
            /* Features and Filters: extend Jersey, manipulate requests and responses. */
            AuthFilter.class,
            CorsFilter.class,
            // Enforce roles annotations defined by JSR-250
            RolesAllowedDynamicFeature.class
        );
    }

    /**
     * Like getClasses, this method declares web resources, providers, and features to the JAX-RS implementation.
     * However, these are single instances that will be reused for all requests (they are singleton-scoped).
     * See https://jersey.java.net/apidocs/latest/jersey/javax/ws/rs/core/Application.html#getSingletons()
     * Leave <Object> out of method signature to avoid confusing the Guava type inference.
     */
    @Override
    public Set getSingletons() {
        return Sets.newHashSet (
            // Show exception messages in responses
            new OTPExceptionMapper(),
            // Serialize POJOs (unannotated) JSON using Jackson
            new JSONObjectMapperProvider(),
            // Allow injecting the OTP server object into Jersey resource classes
            server.makeBinder()
        );
    }

    /**
     * Enabling tracing allows us to see how web resource names were matched from the client, in headers.
     * Disable auto-discovery of features because it's extremely obnoxious to debug and interacts
     * in confusing ways with manually registered features.
     */
    @Override
    public Map<String, Object> getProperties() {
        Map props = Maps.newHashMap();
        props.put(ServerProperties.TRACING, Boolean.TRUE);
        props.put(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.TRUE);
        return props;
    }

}
