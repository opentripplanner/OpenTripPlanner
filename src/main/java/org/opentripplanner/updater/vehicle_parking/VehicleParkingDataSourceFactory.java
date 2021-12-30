package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.ext.vehicleparking.kml.KmlBikeParkDataSource;
import org.opentripplanner.ext.vehicleparking.parkapi.BicycleParkAPIUpdater;
import org.opentripplanner.ext.vehicleparking.parkapi.CarParkAPIUpdater;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.updater.DataSource;

public class VehicleParkingDataSourceFactory {

  private VehicleParkingDataSourceFactory() {}

  public static DataSource<VehicleParking> create(VehicleParkingUpdaterParameters source) {
    switch (source.getSourceType()) {
      case KML:               return new KmlBikeParkDataSource(source.getUrl(), source.getFeedId(), source.getNamePrefix(), source.isZip());
      case PARK_API:          return new CarParkAPIUpdater(source.getUrl(), source.getFeedId(), source.getHttpHeaders(), source.getTags());
      case BICYCLE_PARK_API:  return new BicycleParkAPIUpdater(source.getUrl(), source.getFeedId(), source.getHttpHeaders(), source.getTags());
    }
    throw new IllegalArgumentException(
        "Unknown vehicle parking source type: " + source.getSourceType()
    );
  }
}
