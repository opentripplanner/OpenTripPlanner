package org.opentripplanner.api.configuration;

import org.opentripplanner.api.resource.AlertPatcher;
import org.opentripplanner.api.resource.BikeRental;
import org.opentripplanner.api.resource.ExternalGeocoderResource;
import org.opentripplanner.api.resource.GraphInspectorTileResource;
import org.opentripplanner.api.resource.PlannerResource;
import org.opentripplanner.api.resource.Routers;
import org.opentripplanner.api.resource.ServerInfo;
import org.opentripplanner.api.resource.UpdaterStatusResource;
import org.opentripplanner.ext.examples.statistics.api.resource.GraphStatisticsResource;
import org.opentripplanner.ext.readiness_endpoint.ActuatorAPI;
import org.opentripplanner.ext.transmodelapi.TransmodelIndexAPI;
import org.opentripplanner.index.IndexAPI;
import org.opentripplanner.util.OTPFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.opentripplanner.util.OTPFeature.APIAlertPatcher;
import static org.opentripplanner.util.OTPFeature.APIBikeRental;
import static org.opentripplanner.util.OTPFeature.APIExternalGeocoder;
import static org.opentripplanner.util.OTPFeature.APIGraphInspectorTile;
import static org.opentripplanner.util.OTPFeature.APIServerInfo;
import static org.opentripplanner.util.OTPFeature.APIUpdaterStatus;
import static org.opentripplanner.util.OTPFeature.ActuatorAPI;
import static org.opentripplanner.util.OTPFeature.SandboxAPITransmodelApi;
import static org.opentripplanner.util.OTPFeature.SandboxExampleAPIGraphStatistics;

/**
 * Configure API resource endpoints.
 */
public class APIEndpoints {

    private final List<Class<?>> resources = new ArrayList<>();

    private APIEndpoints() {
        // Add mandatory APIs
        add(Routers.class);
        add(PlannerResource.class);
        add(IndexAPI.class);

        // Add feature enabled APIs, these can be enabled by default, some is not.
        // See the OTPFeature enum for details.
        addIfEnabled(APIExternalGeocoder, ExternalGeocoderResource.class);
        addIfEnabled(APIBikeRental, BikeRental.class);
        addIfEnabled(APIAlertPatcher, AlertPatcher.class);
        addIfEnabled(APIServerInfo, ServerInfo.class);
        addIfEnabled(APIGraphInspectorTile, GraphInspectorTileResource.class);
        addIfEnabled(APIUpdaterStatus, UpdaterStatusResource.class);

        // Sandbox extension APIs
        addIfEnabled(ActuatorAPI, ActuatorAPI.class);
        addIfEnabled(SandboxExampleAPIGraphStatistics, GraphStatisticsResource.class);
        addIfEnabled(SandboxAPITransmodelApi, TransmodelIndexAPI.class);
    }

    /**
     * List all mandatory and feature enabled endpoints as Jersey resource
     * classes: define web services, i.e. an HTTP APIs.
     * <p>
     * Some of the endpoints can be turned on/off using {@link OTPFeature}s, this
     * method check if an endpoint is enabled before adding it to the list.
     *
     * @return all mandatory and feature enabled endpoints are returned.
     */
    public static Collection<? extends Class<?>> listAPIEndpoints() {
        return Collections.unmodifiableCollection(new APIEndpoints().resources);
    }

    /**
     * Add feature to list of classes if the feature is enabled (turned on).
     *
     * @param apiFeature the feature to check.
     * @param resource   the resource to enable if feature is enabled.
     */
    private void addIfEnabled(OTPFeature apiFeature, Class<?> resource) {
        if (apiFeature.isOn()) {
            add(resource);
        }
    }

    private void add(Class<?> resource) {
        resources.add(resource);
    }
}
