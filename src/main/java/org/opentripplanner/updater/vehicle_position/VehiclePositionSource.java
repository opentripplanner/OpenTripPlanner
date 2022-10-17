package org.opentripplanner.updater.vehicle_position;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public interface VehiclePositionSource {
  /**
   * Parses GTFS-RT for vehicle positions
   */
  List<VehiclePosition> getPositions();

  default List<VehiclePosition> getPositions(InputStream is) throws IOException {
    List<VehiclePosition> positions = null;
    List<GtfsRealtime.FeedEntity> feedEntityList;
    GtfsRealtime.FeedMessage feedMessage;

    if (is != null) {
      // Decode message
      feedMessage = GtfsRealtime.FeedMessage.parseFrom(is);
      feedEntityList = feedMessage.getEntityList();

      // Create List of TripUpdates
      positions = new ArrayList<>(feedEntityList.size());
      for (GtfsRealtime.FeedEntity feedEntity : feedEntityList) {
        if (feedEntity.hasVehicle()) {
          positions.add(feedEntity.getVehicle());
        }
      }
    }

    return positions;
  }
}
