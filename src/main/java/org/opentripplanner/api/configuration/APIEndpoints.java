package org.opentripplanner.api.configuration;

import static org.opentripplanner.framework.application.OTPFeature.APIBikeRental;
import static org.opentripplanner.framework.application.OTPFeature.APIGraphInspectorTile;
import static org.opentripplanner.framework.application.OTPFeature.APIServerInfo;
import static org.opentripplanner.framework.application.OTPFeature.APIUpdaterStatus;
import static org.opentripplanner.framework.application.OTPFeature.ActuatorAPI;
import static org.opentripplanner.framework.application.OTPFeature.ReportApi;
import static org.opentripplanner.framework.application.OTPFeature.SandboxAPIGeocoder;
import static org.opentripplanner.framework.application.OTPFeature.SandboxAPILegacyGraphQLApi;
import static org.opentripplanner.framework.application.OTPFeature.SandboxAPIMapboxVectorTilesApi;
import static org.opentripplanner.framework.application.OTPFeature.SandboxAPIParkAndRideApi;
import static org.opentripplanner.framework.application.OTPFeature.SandboxAPITransmodelApi;
import static org.opentripplanner.framework.application.OTPFeature.SandboxAPITravelTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.api.resource.BikeRental;
import org.opentripplanner.api.resource.GraphInspectorTileResource;
import org.opentripplanner.api.resource.GraphInspectorVectorTileResource;
import org.opentripplanner.api.resource.PlannerResource;
import org.opentripplanner.api.resource.Routers;
import org.opentripplanner.api.resource.ServerInfo;
import org.opentripplanner.api.resource.UpdaterStatusResource;
import org.opentripplanner.ext.actuator.ActuatorAPI;
import org.opentripplanner.ext.geocoder.GeocoderResource;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLAPI;
import org.opentripplanner.ext.parkAndRideApi.ParkAndRideResource;
import org.opentripplanner.ext.reportapi.resource.ReportResource;
import org.opentripplanner.ext.transmodelapi.TransmodelAPI;
import org.opentripplanner.ext.traveltime.TravelTimeResource;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.index.IndexAPI;

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
    addIfEnabled(APIBikeRental, BikeRental.class);
    addIfEnabled(APIServerInfo, ServerInfo.class);
    addIfEnabled(APIGraphInspectorTile, GraphInspectorTileResource.class);
    addIfEnabled(APIGraphInspectorTile, GraphInspectorVectorTileResource.class);
    addIfEnabled(APIUpdaterStatus, UpdaterStatusResource.class);

    // Sandbox extension APIs
    addIfEnabled(ActuatorAPI, ActuatorAPI.class);
    addIfEnabled(ReportApi, ReportResource.class);
    addIfEnabled(SandboxAPITransmodelApi, TransmodelAPI.class);
    addIfEnabled(SandboxAPILegacyGraphQLApi, LegacyGraphQLAPI.class);
    addIfEnabled(SandboxAPIMapboxVectorTilesApi, VectorTilesResource.class);
    addIfEnabled(SandboxAPIParkAndRideApi, ParkAndRideResource.class);
    addIfEnabled(SandboxAPIGeocoder, GeocoderResource.class);
    addIfEnabled(SandboxAPITravelTime, TravelTimeResource.class);
  }

  /**
   * List all mandatory and feature enabled endpoints as Jersey resource classes: define web
   * services, i.e. an HTTP APIs.
   * <p>
   * Some of the endpoints can be turned on/off using {@link OTPFeature}s, this method check if an
   * endpoint is enabled before adding it to the list.
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
