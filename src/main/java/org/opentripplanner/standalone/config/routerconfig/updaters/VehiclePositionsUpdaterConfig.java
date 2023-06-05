package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.vehicle_position.VehiclePositionsUpdaterParameters;

public class VehiclePositionsUpdaterConfig {

  public static VehiclePositionsUpdaterParameters create(String updaterRef, NodeAdapter c) {
    var feedId = c
      .of("feedId")
      .since(V2_2)
      .summary("Feed ID to which the update should be applied.")
      .asString();
    var frequency = c
      .of("frequency")
      .since(V2_2)
      .summary("How often the positions should be updated.")
      .asDuration(Duration.ofMinutes(1));
    var url = c
      .of("url")
      .since(V2_2)
      .summary("The URL of GTFS-RT protobuf HTTP resource to download the positions from.")
      .asUri();
    var headers = HttpHeadersConfig.headers(c, V2_3);
    return new VehiclePositionsUpdaterParameters(updaterRef, feedId, url, frequency, headers);
  }
}
