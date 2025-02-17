package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.ext.vehicleparking.bikeep.BikeepUpdater;
import org.opentripplanner.ext.vehicleparking.bikeep.BikeepUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.bikely.BikelyUpdater;
import org.opentripplanner.ext.vehicleparking.bikely.BikelyUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.liipi.LiipiParkUpdater;
import org.opentripplanner.ext.vehicleparking.liipi.LiipiParkUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.parkapi.BicycleParkAPIUpdater;
import org.opentripplanner.ext.vehicleparking.parkapi.CarParkAPIUpdater;
import org.opentripplanner.ext.vehicleparking.parkapi.ParkAPIUpdaterParameters;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.updater.spi.DataSource;

/**
 * Class that can be used to return a custom vehicle parking {@link DataSource}.
 */
public class VehicleParkingDataSourceFactory {

  private VehicleParkingDataSourceFactory() {}

  public static DataSource<VehicleParking> create(
    VehicleParkingUpdaterParameters parameters,
    OpeningHoursCalendarService openingHoursCalendarService
  ) {
    return switch (parameters.sourceType()) {
      case LIIPI -> new LiipiParkUpdater(
        (LiipiParkUpdaterParameters) parameters,
        openingHoursCalendarService
      );
      case PARK_API -> new CarParkAPIUpdater(
        (ParkAPIUpdaterParameters) parameters,
        openingHoursCalendarService
      );
      case BICYCLE_PARK_API -> new BicycleParkAPIUpdater(
        (ParkAPIUpdaterParameters) parameters,
        openingHoursCalendarService
      );
      case BIKELY -> new BikelyUpdater((BikelyUpdaterParameters) parameters);
      case BIKEEP -> new BikeepUpdater((BikeepUpdaterParameters) parameters);
      case SIRI_FM -> throw new IllegalArgumentException("Cannot instantiate SIRI-FM data source");
    };
  }
}
