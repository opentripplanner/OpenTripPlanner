package org.opentripplanner.service.vehiclepositions;

import java.util.List;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition;
import org.opentripplanner.transit.model.network.TripPattern;

public interface VehiclePositionService {
  void setVehiclePositions(TripPattern pattern, List<RealtimeVehiclePosition> updates);

  void clearVehiclePositions(TripPattern pattern);

  List<RealtimeVehiclePosition> getVehiclePositions(TripPattern pattern);
}
