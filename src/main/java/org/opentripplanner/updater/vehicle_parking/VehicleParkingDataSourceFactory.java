package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.ext.vehicleparking.bikely.BikelyUpdater;
import org.opentripplanner.ext.vehicleparking.bikely.BikelyUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.hslpark.HslParkUpdater;
import org.opentripplanner.ext.vehicleparking.hslpark.HslParkUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.noi.NoiUpdater;
import org.opentripplanner.ext.vehicleparking.noi.NoiUpdaterParameters;
import org.opentripplanner.ext.vehicleparking.parkapi.BicycleParkAPIUpdater;
import org.opentripplanner.ext.vehicleparking.parkapi.CarParkAPIUpdater;
import org.opentripplanner.ext.vehicleparking.parkapi.ParkAPIUpdaterParameters;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
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
      case HSL_PARK -> new HslParkUpdater(
        (HslParkUpdaterParameters) parameters,
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
      case NOI_OPEN_DATA_HUB -> new NoiUpdater((NoiUpdaterParameters) parameters);
    };
  }
}
