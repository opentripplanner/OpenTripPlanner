package org.opentripplanner.updater.vehicle_position;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for downloading GTFS-rt vehicle positions from a URL and loading into memory.
 */
public class GtfsRealtimeHttpVehiclePositionSource implements VehiclePositionSource {

  private static final Logger LOG = LoggerFactory.getLogger(
    GtfsRealtimeHttpVehiclePositionSource.class
  );

  /**
   * URL to grab GTFS-RT feed from
   */
  private final URI url;

  private final Map<String, String> defaultHeaders = Map.of(
    "Accept",
    "application/x-google-protobuf, application/x-protobuf, application/protobuf, application/octet-stream, */*"
  );

  public GtfsRealtimeHttpVehiclePositionSource(URI url) {
    this.url = url;
  }

  /**
   * Parses raw GTFS-RT data into vehicle positions
   */
  public List<VehiclePosition> getPositions() {
    try (InputStream is = HttpUtils.openInputStream(url.toString(), defaultHeaders)) {
      if (is == null) {
        LOG.warn("Failed to get data from url {}", url);
        return List.of();
      }
      return this.getPositions(is);
    } catch (IOException e) {
      LOG.warn("Error reading vehicle positions from {}", url, e);
    }
    return List.of();
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(GtfsRealtimeHttpVehiclePositionSource.class)
      .addObj("url", url)
      .toString();
  }
}
