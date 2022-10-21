package org.opentripplanner.ext.vehicleparking.parkapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;

/**
 * Vehicle parking updater class that extends the {@link ParkAPIUpdater}. Meant for reading bicycle
 * parks from https://github.com/offenesdresden/ParkAPI format APIs.
 */
public class BicycleParkAPIUpdater extends ParkAPIUpdater {

  public BicycleParkAPIUpdater(
    ParkAPIUpdaterParameters parameters,
    OpeningHoursCalendarService openingHoursCalendarService
  ) {
    super(parameters, openingHoursCalendarService);
  }

  @Override
  protected VehicleParkingSpaces parseCapacity(JsonNode jsonNode) {
    return parseVehicleSpaces(jsonNode, "total", null, null);
  }

  @Override
  protected VehicleParkingSpaces parseAvailability(JsonNode jsonNode) {
    return parseVehicleSpaces(jsonNode, "free", null, null);
  }
}
