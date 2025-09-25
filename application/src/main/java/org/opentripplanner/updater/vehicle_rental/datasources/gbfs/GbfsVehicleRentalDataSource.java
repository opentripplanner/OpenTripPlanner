package org.opentripplanner.updater.vehicle_rental.datasources.gbfs;

import java.util.List;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

/**
 * Created by demory on 2017-03-14.
 * <p>
 * Leaving OTPFeature.FloatingBike turned off both prevents floating bike updaters added to
 * router-config.json from being used, but more importantly, floating bikes added by a
 * VehicleRentalServiceDirectoryFetcher endpoint (which may be outside our control) will not be
 * used.
 */
public class GbfsVehicleRentalDataSource implements VehicleRentalDataSource {

  private final GbfsVehicleRentalDataSourceParameters params;
  private final OtpHttpClientFactory otpHttpClientFactory;
  private GbfsFeedLoaderAndMapper loaderAndMapper = null;

  public GbfsVehicleRentalDataSource(
    GbfsVehicleRentalDataSourceParameters parameters,
    OtpHttpClientFactory otpHttpClientFactory
  ) {
    this.params = parameters;
    this.otpHttpClientFactory = otpHttpClientFactory;
  }

  @Override
  public void setup() {
    loaderAndMapper = new GbfsFeedLoaderAndMapper(params, otpHttpClientFactory);
  }

  @Override
  public boolean update() {
    if (loaderAndMapper == null) {
      return false;
    }
    return loaderAndMapper.update();
  }

  @Override
  public List<VehicleRentalPlace> getUpdates() {
    return loaderAndMapper.getUpdated();
  }

  @Override
  public List<GeofencingZone> getGeofencingZones() {
    return loaderAndMapper.getGeofencingZones();
  }
}
