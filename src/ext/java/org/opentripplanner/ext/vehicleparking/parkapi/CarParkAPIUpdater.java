package org.opentripplanner.ext.vehicleparking.parkapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;

/**
 * Vehicle parking updater class that extends the {@link ParkAPIUpdater}. Meant for reading car
 * parks from https://github.com/offenesdresden/ParkAPI format APIs.
 */
public class CarParkAPIUpdater extends ParkAPIUpdater {

  public CarParkAPIUpdater(
    ParkAPIUpdaterParameters parameters,
    OpeningHoursCalendarService openingHoursCalendarService
  ) {
    super(parameters, openingHoursCalendarService);
  }

  @Override
  protected VehicleParkingSpaces parseCapacity(JsonNode jsonNode) {
    return parseVehicleSpaces(jsonNode, null, "total", "total:disabled");
  }

  @Override
  protected VehicleParkingSpaces parseAvailability(JsonNode jsonNode) {
    return parseVehicleSpaces(jsonNode, null, "free", "free:disabled");
  }
}
