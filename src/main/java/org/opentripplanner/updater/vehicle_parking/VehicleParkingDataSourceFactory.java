package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.ext.vehicleparking.hslpark.HslParkUpdater;
import org.opentripplanner.ext.vehicleparking.hslpark.HslParkUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.kml.KmlBikeParkDataSource;
import org.opentripplanner.ext.vehicleparking.kml.KmlUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.parkapi.BicycleParkAPIUpdater;
import org.opentripplanner.ext.vehicleparking.parkapi.CarParkAPIUpdater;
import org.opentripplanner.ext.vehicleparking.parkapi.ParkAPIUpdaterParameters;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.updater.DataSource;

/**
 * Class that can be used to return a custom vehicle parking {@link DataSource}.
 */
public class VehicleParkingDataSourceFactory {

  private VehicleParkingDataSourceFactory() {}

  public static DataSource<VehicleParking> create(VehicleParkingUpdaterParameters parameters) {
    switch (parameters.getSourceType()) {
      case HSL_PARK:
        return new HslParkUpdater((HslParkUpdaterParameters) parameters);
      case KML:
        return new KmlBikeParkDataSource((KmlUpdaterParameters) parameters);
      case PARK_API:
        return new CarParkAPIUpdater((ParkAPIUpdaterParameters) parameters);
      case BICYCLE_PARK_API:
        return new BicycleParkAPIUpdater((ParkAPIUpdaterParameters) parameters);
    }
    throw new IllegalArgumentException(
            "Unknown vehicle parking source type: " + parameters.getSourceType()
    );
  }
}
