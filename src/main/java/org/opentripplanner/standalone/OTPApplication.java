package org.opentripplanner.standalone;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.opentripplanner.api.common.OTPExceptionMapper;
import org.opentripplanner.api.model.JSONObjectMapperProvider;
import org.opentripplanner.api.resource.*;
import org.opentripplanner.index.GeocoderResource;
import org.opentripplanner.index.IndexAPI;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.core.Application;

import java.util.Arrays;
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
        // Bridge j.u.l (used by Jersey) to the SLF4J root logger, so all logging goes through the same API
        SLF4JBridgeHandler.install();
    }

    /* This object groups together all the modules for a single running OTP server. */
    public final OTPServer server;

    /* If secure is true, OTP will require Basic authentication over HTTPS when accessing dangerous web services. */
    private final boolean secure;

    /**
     * The OTPServer provides entry points to OTP routing functionality for a collection of OTPRouters.
     * It provides a Java API, not an HTTP API.
     * The OTPApplication wraps an OTPServer in a Jersey (JAX-RS) Application, configuring an HTTP API.
     * @param server The OTP server to wrap
     * @param secure Should this server require authentication over HTTPS to access secure resources, e.g. /routers?
     */
    public OTPApplication (OTPServer server, boolean secure) {
        this.server = server;
        this.secure = secure;
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
        Set<Class<?>> classes = Sets.newHashSet();
        classes.addAll(Arrays.asList(
            /* Jersey resource classes: define web services, i.e. an HTTP API. */
            PlannerResource.class,
            IndexAPI.class,
            ExternalGeocoderResource.class,
            GeocoderResource.class,
            SimpleIsochrone.class,
            TileService.class,
            BikeRental.class,
            LIsochrone.class,
            ExternalGeocoderResource.class,
            TimeGridWs.class,
            AlertPatcher.class,
            PlannerResource.class,
            SIsochrone.class,
            Routers.class,
            LegendResource.class,
            ProfileResource.class,
            SimpleIsochrone.class,
            ServerInfo.class,
            SurfaceResource.class,
            PointSetResource.class,
            GraphInspectorTileResource.class,
            ScriptResource.class,
            UpdaterStatusResource.class,
            ScenarioResource.class,
            RepeatedRaptorTestResource.class,
            /* Features and Filters: extend Jersey, manipulate requests and responses. */
            CorsFilter.class,
            MultiPartFeature.class
        ));
        
        if (this.secure) {
            // A filter that converts HTTP Basic authentication headers into a Jersey SecurityContext
            classes.add(AuthFilter.class);
            // Enforce roles annotations defined by JSR-250 (allow access to API methods based on the SecurityContext)
            classes.add(RolesAllowedDynamicFeature.class);
        }
        
        return classes;
    }

    /**
     * Like getClasses, this method declares web resources, providers, and features to the JAX-RS implementation.
     * However, these are single instances that will be reused for all requests (they are singleton-scoped).
     * See https://jersey.java.net/apidocs/latest/jersey/javax/ws/rs/core/Application.html#getSingletons()
     * Leave <Object> out of method signature to avoid confusing the Guava type inference.
     */
    @Override
    public Set<Object> getSingletons() {
        return Sets.newHashSet (
            // Show exception messages in responses
            new OTPExceptionMapper(),
            // Enable Jackson JSON response serialization
            new JacksonJsonProvider(),
            // Enable Jackson XML response serialization
            new JacksonXMLProvider(),
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
    // @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> props = Maps.newHashMap();
        props.put(ServerProperties.TRACING, Boolean.TRUE);
        props.put(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.TRUE);
        return props;
    }

}
