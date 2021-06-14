package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.updater.DataSource;

public class VehicleParkingDataSourceFactory {

  private VehicleParkingDataSourceFactory() {}

  public static DataSource<VehicleParking> create(VehicleParkingUpdaterParameters source) {
    switch (source.getSourceType()) {
      case KML:        return new KmlBikeParkDataSource(source.getUrl(), source.getFeedId(), source.getNamePrefix(), source.isZip());
    }
    throw new IllegalArgumentException(
        "Unknown vehicle parking source type: " + source.getSourceType()
    );
  }
}
