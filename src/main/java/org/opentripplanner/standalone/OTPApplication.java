package org.opentripplanner.standalone;

import com.google.common.collect.Sets;
import org.opentripplanner.api.resource.BikeRental;
import org.opentripplanner.api.resource.GeocoderResource;
import org.opentripplanner.api.resource.Metadata;
import org.opentripplanner.api.resource.Patcher;
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
import org.opentripplanner.api.resource.analyst.WebMapService;
import org.opentripplanner.api.ws.analyst.TimeGridWs;

import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * A Jersey Application subclass which provides hard-wired configuration of an OTP server. Avoids injection of any kind.
 */
public class OTPApplication extends Application {

    public final OTPServer server;

    public OTPApplication () {
        server = new OTPServer();
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
            Patcher.class,
            Planner.class,
            SIsochrone.class,
            Routers.class,
            Raster.class,
            LegendResource.class,
            Metadata.class,
            ProfileEndpoint.class,
            SimpleIsochrone.class,
            ServerInfo.class
            // Enable Jackson POJO JSON serialization
            // JacksonFeature.class
        );

        // JerseyInjector
        // replace with simple Parameter classes that have String constructors.
        // CRSStringReaderProvider.class,
        // EnvelopeStringReaderProvider.class

    }
}
