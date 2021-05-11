package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class ParkAndRideEntranceRemoved implements DataImportIssue {

  private static final String FMT = "Park and ride entrance '%s' is removed because it's StreetVertex ('%s') is removed in a previous step.";

  private final String entranceId;
  private final String streetVertexName;

  public ParkAndRideEntranceRemoved(VehicleParking.VehicleParkingEntrance vehicleParkingEntrance){
    this.entranceId = vehicleParkingEntrance.getEntranceId().toString();
    this.streetVertexName = vehicleParkingEntrance.getVertex().getName();
  }

  @Override
  public String getMessage() {
    return String.format(FMT, entranceId, streetVertexName);
  }

  @Override
  public String getHTMLMessage() {
    return getMessage();
  }
}
